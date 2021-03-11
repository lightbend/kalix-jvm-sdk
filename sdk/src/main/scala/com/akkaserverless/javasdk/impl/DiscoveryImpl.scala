/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl

import akka.actor.ActorSystem
import com.akkaserverless.javasdk.{BuildInfo, EntityOptions, Service}
import com.akkaserverless.protocol.action.Actions
import com.akkaserverless.protocol.discovery.PassivationStrategy.Strategy
import com.akkaserverless.protocol.discovery._
import com.google.protobuf.DescriptorProtos

import java.time.Duration
import scala.concurrent.Future

class DiscoveryImpl(system: ActorSystem, services: Map[String, Service]) extends Discovery {

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
    system.log.info(
      s"Received discovery call from [${in.proxyName} ${in.proxyVersion}] supporting Akka Serverless protocol ${in.protocolMajorVersion}.${in.protocolMinorVersion}"
    )
    system.log.debug(s"Supported sidecar entity types: ${in.supportedEntityTypes.mkString("[", ",", "]")}")

    val unsupportedServices = services.values.filterNot { service =>
      in.supportedEntityTypes.contains(service.componentType)
    }

    if (unsupportedServices.nonEmpty) {
      system.log.error(
        "Proxy doesn't support the entity types for the following services: " + unsupportedServices
          .map(s => s.descriptor.getFullName + ": " + s.componentType)
          .mkString(", ")
      )
      // Don't fail though. The proxy may give us more information as to why it doesn't support them if we send back unsupported services.
      // eg, the proxy doesn't have a configured journal, and so can't support event sourcing.
    }

    val allDescriptors = AnySupport.flattenDescriptors(services.values.map(_.descriptor.getFile).toSeq)
    val builder = DescriptorProtos.FileDescriptorSet.newBuilder()
    allDescriptors.values.foreach(fd => builder.addFile(fd.toProto))
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
    system.log.error(s"Error reported from sidecar: ${in.message}")
    Future.successful(com.google.protobuf.empty.Empty.defaultInstance)
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
}
