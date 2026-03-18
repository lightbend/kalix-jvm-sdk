/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.backoffice

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import io.grpc.{ CallCredentials, Metadata, Status }
import kalix.protocol.discovery.BackofficeService
import org.slf4j.LoggerFactory

import java.util.concurrent.Executor
import scala.util.{ Failure, Success }

object BackofficeServiceSupport {

  private val log = LoggerFactory.getLogger(getClass)

  def grpcClientFor(name: String, settings: BackofficeService, system: ActorSystem): GrpcClientSettings = {

    val (backofficeHost, backofficePort) = settings.backofficeProxyHost.split(":", 2) match {
      case Array(host)       => (host, 443)
      case Array(host, port) => (host, port.toInt)
      case _                 => (settings.backofficeProxyHost, 443)
    }

    log.info(
      s"Using cloud service ${settings.serviceName} in project ${settings.projectId} in region ${settings.regionName} for service to service eventing to $name")
    GrpcClientSettings
      .connectToServiceAt(backofficeHost, backofficePort)(system)
      .withCallCredentials(new BackofficeCallCredentials(settings, system))
  }

  private val KalixProxyHost = Metadata.Key.of("kalix-proxy-host", Metadata.ASCII_STRING_MARSHALLER)
  private val KalixProxyAuthorization = Metadata.Key.of("kalix-proxy-authorization", Metadata.ASCII_STRING_MARSHALLER)

  private class BackofficeCallCredentials(settings: BackofficeService, system: ActorSystem) extends CallCredentials {

    override def applyRequestMetadata(
        requestInfo: CallCredentials.RequestInfo,
        appExecutor: Executor,
        applier: CallCredentials.MetadataApplier): Unit = {
      import system.dispatcher
      BackofficeAccessTokenCache(system).accessToken().onComplete {
        case Success(accessToken) =>
          val headers = new Metadata()
          val host = s"${settings.serviceName}.${settings.projectId}.svc.kalix.local"
          headers.put(KalixProxyHost, host)
          headers.put(KalixProxyAuthorization, accessToken)
          applier(headers)
        case Failure(exception) =>
          applier.fail(Status.INTERNAL.withCause(exception))
      }
    }
  }
}
