/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

import akka.Done
import akka.actor.ActorSystem
import akka.actor.ClassicActorSystemProvider
import akka.actor.CoordinatedShutdown
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.grpc.GrpcClientSettings
import akka.grpc.javadsl.{ AkkaGrpcClient => AkkaGrpcJavaClient }
import akka.grpc.scaladsl.{ AkkaGrpcClient => AkkaGrpcScalaClient }
import io.grpc.CallCredentials
import io.grpc.Metadata
import org.slf4j.LoggerFactory

/**
 * INTERNAL API
 */
object GrpcClients extends ExtensionId[GrpcClients] with ExtensionIdProvider {
  override def get(system: ActorSystem): GrpcClients = super.get(system)

  override def get(system: ClassicActorSystemProvider): GrpcClients = super.get(system)

  override def createExtension(system: ExtendedActorSystem): GrpcClients =
    new GrpcClients(system)
  override def lookup: ExtensionId[_ <: Extension] = this

  final private case class Key(serviceClass: Class[_], service: String, port: Int, addHeader: Option[(String, String)])
}

/**
 * INTERNAL API
 */
final class GrpcClients(system: ExtendedActorSystem) extends Extension {
  import GrpcClients._
  private val log = LoggerFactory.getLogger(classOf[GrpcClients])

  private val proxyInfoHolder = ProxyInfoHolder(system)
  private implicit val ec: ExecutionContext = system.dispatcher
  private val clients = new ConcurrentHashMap[Key, AnyRef]()
  private val MaxCrossServiceResponseContentLength =
    system.settings.config.getBytes("kalix.cross-service.max-content-length").toInt

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceStop, "stop-grpc-clients")(() =>
    Future
      .traverse(clients.values().asScala) {
        case javaClient: AkkaGrpcJavaClient   => javaClient.close().asScala
        case scalaClient: AkkaGrpcScalaClient => scalaClient.close()
        case _                                =>
          // should never happen, but needs to make compiler happy
          throw new IllegalStateException("Unknown gRPC client")
      }
      .map(_ => Done))

  def getComponentGrpcClient[T](serviceClass: Class[T]): T = {
    getProxyGrpcClient(serviceClass)
  }
  def getProxyGrpcClient[T](serviceClass: Class[T]): T = {
    getLocalGrpcClient(serviceClass)
  }

  /**
   * This gets called from the action context to get a client to another service, and hence needs to add a service
   * identification header (in dev/test mode) to ensure calls get associated with this service.
   */
  def getGrpcClient[T](serviceClass: Class[T], service: String): T = {
    val remoteAddHeader = proxyInfoHolder.remoteIdentificationHeader
    getGrpcClient(serviceClass, service, port = 80, remoteAddHeader)
  }

  /** gRPC clients point to services (user components or Kalix services) in the same deployable */
  private def getLocalGrpcClient[T](serviceClass: Class[T]): T = {

    val localAddHeader = proxyInfoHolder.localIdentificationHeader

    (proxyInfoHolder.proxyHostname, proxyInfoHolder.proxyPort) match {
      case (Some(internalProxyHostname), Some(port)) =>
        getGrpcClient(serviceClass, internalProxyHostname, port, localAddHeader)
      case (Some("localhost"), None) =>
        // for backward compatibility with proxy 1.0.14 or older.
        log.warn("you are using an old version of the Kalix Runtime")
        getGrpcClient(serviceClass, "localhost", proxyInfoHolder.proxyPort.getOrElse(9000), localAddHeader)
      case (Some(proxyHostname), None) =>
        // for backward compatibility with proxy 1.0.14 or older
        log.warn("you are using an old version of the Kalix Runtime")
        getGrpcClient(serviceClass, proxyHostname, 80, localAddHeader)
      case _ =>
        throw new IllegalStateException(
          "Service proxy hostname and port are not set by proxy at discovery, too old proxy version?")
    }
  }

  /** This gets called by the testkit, so shouldn't add any headers. */
  def getGrpcClient[T](serviceClass: Class[T], service: String, port: Int): T =
    getGrpcClient(serviceClass, service, port, None)

  /** This gets called by the testkit, and should impersonate the given principal. */
  def getGrpcClient[T](serviceClass: Class[T], service: String, port: Int, impersonate: String): T =
    getGrpcClient(serviceClass, service, port, Some("impersonate-kalix-service" -> impersonate))

  private def getGrpcClient[T](
      serviceClass: Class[T],
      service: String,
      port: Int,
      addHeader: Option[(String, String)]) = {
    clients.computeIfAbsent(Key(serviceClass, service, port, addHeader), createClient(_)).asInstanceOf[T]
  }

  private def createClient(key: Key): AnyRef = {
    val settings =
      if (!system.settings.config.hasPath(s"""akka.grpc.client."${key.service}"""")) {
        // "service" is not present in the config, treat it as an Akka gRPC inter-service call
        log.debug("Creating gRPC client for Kalix service [{}:{}]", key.service, key.port)
        GrpcClientSettings
          .connectToServiceAt(key.service, key.port)(system)
          // (TLS is handled for us by Kalix infra)
          .withTls(false)
          .withChannelBuilderOverrides(channelBuilder =>
            channelBuilder.maxInboundMessageSize(MaxCrossServiceResponseContentLength))
      } else {
        log.debug("Creating gRPC client for external service [{}]", key.service)
        // external service, defined in config
        GrpcClientSettings.fromConfig(key.service)(system)
      }

    val settingsWithCallCredentials = key.addHeader match {
      case Some((key, value)) =>
        val headers = new Metadata()
        headers.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)
        settings.withCallCredentials(new CallCredentials {
          override def applyRequestMetadata(
              requestInfo: CallCredentials.RequestInfo,
              appExecutor: Executor,
              applier: CallCredentials.MetadataApplier): Unit = {
            applier.apply(headers)
          }
          override def thisUsesUnstableApi(): Unit = ()
        })
      case None => settings
    }

    // expected to have a ServiceNameClient generated in the same package, so look that up through reflection
    val clientClass = system.dynamicAccess.getClassFor[AnyRef](key.serviceClass.getName + "Client").get
    val client =
      if (classOf[AkkaGrpcJavaClient].isAssignableFrom(clientClass)) {
        // Java API - static create
        val create = clientClass.getMethod("create", classOf[GrpcClientSettings], classOf[ClassicActorSystemProvider])
        create.invoke(null, settingsWithCallCredentials, system)
      } else if (classOf[AkkaGrpcScalaClient].isAssignableFrom(clientClass)) {
        // Scala API - companion object apply
        val companion = system.dynamicAccess.getObjectFor[AnyRef](key.serviceClass.getName + "Client").get
        val create =
          companion.getClass.getMethod("apply", classOf[GrpcClientSettings], classOf[ClassicActorSystemProvider])
        create.invoke(companion, settingsWithCallCredentials, system)
      } else {
        throw new IllegalArgumentException(s"Expected an AkkaGrpcClient but was [${clientClass.getName}]")
      }

    val closeDone = client match {
      case javaClient: AkkaGrpcJavaClient =>
        javaClient.closed().asScala
      case scalaClient: AkkaGrpcScalaClient =>
        scalaClient.closed
      case _ =>
        // should never happen, but needs to make compiler happy
        throw new IllegalStateException("Unknown gRPC client")
    }
    closeDone.foreach { _ =>
      // if the client is closed, remove it from the pool
      log.debug("gRPC client for service [{}] was closed", key.service)
      clients.remove(key)
    }

    client
  }

}
