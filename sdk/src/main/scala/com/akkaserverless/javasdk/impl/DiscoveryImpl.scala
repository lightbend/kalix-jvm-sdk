/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl

import java.nio.file.Files

import akka.actor.ActorSystem
import com.akkaserverless.javasdk.{BuildInfo, EntityOptions, Service}
import com.akkaserverless.protocol.action.Actions
import com.akkaserverless.protocol.discovery.PassivationStrategy.Strategy
import com.akkaserverless.protocol.discovery._
import com.google.protobuf.DescriptorProtos
import java.time.Duration

import scala.concurrent.Future
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import org.slf4j.LoggerFactory

class DiscoveryImpl(system: ActorSystem, services: Map[String, Service]) extends Discovery {

  private val log = LoggerFactory.getLogger(getClass)

  private def configuredOrElse(key: String, default: String): String =
    if (system.settings.config.hasPath(key)) system.settings.config.getString(key) else default

  private def configuredIntOrElse(key: String, default: Int): Int =
    if (system.settings.config.hasPath(key)) system.settings.config.getInt(key) else default

  private val serviceInfo = ServiceInfo(
    serviceRuntime = sys.props.getOrElse("java.runtime.name", "")
      + " " + sys.props.getOrElse("java.runtime.version", ""),
    supportLibraryName = configuredOrElse("akkaserverless.library.name", BuildInfo.name),
    supportLibraryVersion = configuredOrElse("akkaserverless.library.version", BuildInfo.version),
    protocolMajorVersion =
      configuredIntOrElse("akkaserverless.library.protocol-major-version", BuildInfo.protocolMajorVersion),
    protocolMinorVersion =
      configuredIntOrElse("akkaserverless.library.protocol-minor-version", BuildInfo.protocolMinorVersion)
  )

  /**
   * Discover what components the user function wishes to serve.
   */
  override def discover(in: ProxyInfo): scala.concurrent.Future[Spec] = {
    log.info(
      "Received discovery call from [{} {}] supporting Akka Serverless protocol {}.{}",
      in.proxyName,
      in.proxyVersion,
      in.protocolMajorVersion,
      in.protocolMinorVersion
    )
    log.debug(s"Supported sidecar entity types: {}", in.supportedEntityTypes.mkString("[", ",", "]"))

    val unsupportedServices = services.values.filterNot { service =>
      in.supportedEntityTypes.contains(service.componentType)
    }

    if (unsupportedServices.nonEmpty) {
      log.error(
        "Proxy doesn't support the entity types for the following services: {}",
        unsupportedServices
          .map(s => s.descriptor.getFullName + ": " + s.componentType)
          .mkString(", ")
      )
      // Don't fail though. The proxy may give us more information as to why it doesn't support them if we send back unsupported services.
      // eg, the proxy doesn't have a configured journal, and so can't support event sourcing.
    }

    val descriptorsWithSource = loadDescriptorsWithSource(
      system.settings.config.getString("akkaserverless.discovery.protobuf-descriptor-with-source-info-path")
    )
    val allDescriptors = AnySupport.flattenDescriptors(services.values.map(_.descriptor.getFile).toSeq)
    val builder = DescriptorProtos.FileDescriptorSet.newBuilder()
    allDescriptors.values.foreach { fd =>
      val proto = fd.toProto
      // We still use the descriptor as passed in by the user, but if we have one that we've read from the
      // descriptors file that has the source info, we add that source info to the one passed in, and use that.
      val protoWithSource = descriptorsWithSource.get(proto.getName).fold(proto) { withSource =>
        proto.toBuilder.setSourceCodeInfo(withSource.getSourceCodeInfo).build()
      }
      builder.addFile(protoWithSource)
    }
    val fileDescriptorSet = builder.build().toByteString

    val components = services.map {
      case (name, service) =>
        service.componentType match {
          case Actions.name =>
            Component(service.componentType, name, Component.ComponentSettings.Empty)
          case _ =>
            val passivationStrategy = entityPassivationStrategy(service.entityOptions)
            Component(service.componentType,
                      name,
                      Component.ComponentSettings.Entity(EntitySettings(service.entityType, passivationStrategy)))
        }
    }.toSeq

    Future.successful(Spec(fileDescriptorSet, components, Some(serviceInfo)))
  }

  /**
   * Report an error back to the user function. This will only be invoked to tell the user function
   * that it has done something wrong, eg, violated the protocol, tried to use an entity type that
   * isn't supported, or attempted to forward to an entity that doesn't exist, etc. These messages
   * should be logged clearly for debugging purposes.
   */
  override def reportError(in: UserFunctionError): scala.concurrent.Future[com.google.protobuf.empty.Empty] = {
    val sourceMsgs = in.sourceLocations.map { location =>
      loadSource(location) match {
        case None if location.startLine == 0 && location.startCol == 0 =>
          s"At ${location.fileName}"
        case None =>
          s"At ${location.fileName}:${location.startLine + 1}:${location.startCol + 1}"
        case Some(source) =>
          s"At ${location.fileName}:${location.startLine + 1}:${location.startCol + 1}:${"\n"}$source"
      }
    }.toList
    val message = s"Error reported from Akka Serverless system: ${in.code} ${in.message}"
    val detail = if (in.detail.isEmpty) Nil else List(in.detail)
    val seeDocs = DocLinks.forErrorCode(in.code).map(link => s"See documentation: $link").toList
    val messages = message :: detail ::: seeDocs ::: sourceMsgs

    log.error(messages.mkString("\n\n"))

    Future.successful(com.google.protobuf.empty.Empty.defaultInstance)
  }

  private def loadSource(location: UserFunctionError.SourceLocation): Option[String] =
    if (location.endLine == 0 && location.endCol == 0) {
      // It's been sent without line/col data
      None
    } else {
      val resourceStream = getClass.getClassLoader.getResourceAsStream(location.fileName)
      if (resourceStream != null) {
        val lines = Source
          .fromInputStream(resourceStream, "utf-8")
          .getLines()
          .slice(location.startLine, location.endLine + 1)
          .take(6) // Don't render more than 6 lines, we don't want to fill the logs too much
          .toList
        if (lines.size > 1) {
          Some(lines.mkString("\n"))
        } else {
          lines.headOption
            .map { line =>
              line + "\n" + line.take(location.startCol).map {
                case '\t' => '\t'
                case _ => ' '
              } + "^"
            }
        }
      } else None
    }

  private def entityPassivationStrategy(maybeOptions: Option[EntityOptions]): Option[PassivationStrategy] = {
    import com.akkaserverless.protocol.discovery.{PassivationStrategy => EPStrategy}
    maybeOptions.flatMap { options =>
      options.passivationStrategy() match {
        case Timeout(maybeTimeout) =>
          maybeTimeout match {
            case Some(timeout) => Some(EPStrategy(Strategy.Timeout(TimeoutPassivationStrategy(timeout.toMillis))))
            case _ =>
              configuredPassivationTimeout("akkaserverless.passivation-timeout").map(
                timeout => EPStrategy(Strategy.Timeout(TimeoutPassivationStrategy(timeout.toMillis)))
              )
          }
      }
    }
  }

  private def configuredPassivationTimeout(key: String): Option[Duration] =
    if (system.settings.config.hasPath(key)) Some(system.settings.config.getDuration(key)) else None

  private def loadDescriptorsWithSource(path: String): Map[String, DescriptorProtos.FileDescriptorProto] =
    // Special case for disabled, this allows the user to disable attempting to load the descriptor, which means
    // they won't get the great big warning below if it doesn't exist.
    if (path == "disabled") {
      Map.empty
    } else {
      val stream = getClass.getClassLoader.getResourceAsStream(path)
      if (stream == null) {
        log.warn(
          s"Source info descriptor [$path] not found on classpath. Reporting descriptor errors against " +
          "source locations will be disabled. To fix this, ensure that the following configuration applied to the " +
          "protobuf maven plugin: \n" +
          s"""
             |<writeDescriptorSet>true</writeDescriptorSet>
             |<includeSourceInfoInDescriptorSet>true</includeSourceInfoInDescriptorSet>
             |<descriptorSetFileName>${path.split("/").last}</descriptorSetFileName>
             |
             |and also that the generated resources directory is included in the classpath:
             |
             |  <build>
             |    <resources>
             |      <resource>
             |        <directory>$${project.build.directory}/generated-resources</directory>
             |      </resource>
             |    </resources>
             |    ...
             |""".stripMargin
        )
        Map.empty
      } else {
        try {
          DescriptorProtos.FileDescriptorSet
            .parseFrom(stream)
            .getFileList
            .asScala
            .collect {
              case file if file.hasSourceCodeInfo => file.getName -> file
            }
            .toMap
        } catch {
          case NonFatal(e) =>
            log.error("Error parsing descriptor file [{}] from classpath, source mapping will be disabled", path, e)
            Map.empty
        }
      }
    }
}
