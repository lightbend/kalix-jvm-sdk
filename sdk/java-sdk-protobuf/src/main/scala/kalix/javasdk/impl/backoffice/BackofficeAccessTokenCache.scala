/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.backoffice

import akka.actor.{ Actor, ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, Props }
import akka.annotation.InternalApi
import akka.grpc.GrpcClientSettings
import akka.pattern.{ ask, StatusReply }
import akka.util.Timeout
import kalix.api.auth.v1alpha.auth.{ AuthClient, CreateAccessTokenRequest }
import org.slf4j.LoggerFactory

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.DurationConverters.JavaDurationOps
import scala.util.{ Failure, Success }

/**
 * This is primarily used in dev mode, when interacting with other services via the backoffice proxy.
 */
@InternalApi
private[impl] object BackofficeAccessTokenCache extends ExtensionId[BackofficeAccessTokenCache] {

  override def createExtension(system: ExtendedActorSystem): BackofficeAccessTokenCache = {
    val requestTimeout = system.settings.config.getDuration("kalix.dev-mode.backoffice.request-timeout").toScala
    new BackofficeAccessTokenCache(system, requestTimeout)
  }

  override def get(system: ActorSystem): BackofficeAccessTokenCache = apply(system)

  private[impl] sealed trait Protocol
  private[impl] case class Init(apiServerHost: String, apiServerPort: Int, refreshToken: String) extends Protocol
  private[impl] case object GetAccessToken extends Protocol
  private[impl] case class AccessToken(token: String, expiry: Instant) extends Protocol
  private[impl] case class GetFailed(exception: Throwable) extends Protocol
}

@InternalApi
private[impl] class BackofficeAccessTokenCache private (sys: ExtendedActorSystem, requestTimeout: FiniteDuration)
    extends Extension {

  import BackofficeAccessTokenCache._

  private val actor =
    sys.systemActorOf(Props(new BackofficeAccessTokenCacheActor(requestTimeout)), "sdk-backoffice-access-token-cache")
  // Slightly longer than request timeout so that the request timeout failure should be propagated before the ask
  // times out
  private implicit val askTimeout: Timeout = Timeout(requestTimeout.plus(100.millis))

  def init(apiServerHost: String, apiServerPort: Int, refreshToken: String): Unit = {
    actor ! Init(apiServerHost, apiServerPort, refreshToken)
  }

  def accessToken(): Future[String] = {
    actor.askWithStatus(GetAccessToken).mapTo[String]
  }
}

private class BackofficeAccessTokenCacheActor(requestTimeout: FiniteDuration) extends Actor {

  private val log = LoggerFactory.getLogger(getClass)

  import BackofficeAccessTokenCache._

  import context.dispatcher

  override def receive: Receive = {
    case Init(apiServerHost, apiServerPort, refreshToken) =>
      // We log the first 4 characters of the refresh token, which is not sensitive, it should be "kxr_", this will
      // verify that it is a refresh token.
      log.debug(
        s"BackofficeAccessTokenCache initialized with refresh token starting with {}... using server {}:{}",
        refreshToken.take(4),
        apiServerHost,
        apiServerPort)
      val authClient =
        AuthClient(GrpcClientSettings.connectToServiceAt(apiServerHost, apiServerPort)(context.system))(context.system)
      context.become(new Initialized(authClient, refreshToken).idle)

    case GetAccessToken =>
      sender() ! StatusReply.error("Cannot use BackofficeAccessTokenCache until it has been initialized")

    case other =>
      log.debug("Unexpected message while awaiting init: {}", other)
  }

  private class Initialized(authClient: AuthClient, refreshToken: String) {
    def idle: Receive = {
      case GetAccessToken =>
        log.debug("Fetching access token")
        fetchAccessToken()
        context.become(loading(Seq(sender())))
      case msg =>
        log.debug("Unexpected message while idle: {}", msg)
    }

    private def loading(waiting: Seq[ActorRef]): Receive = {
      case GetAccessToken =>
        context.become(loading(waiting :+ sender()))

      case AccessToken(token, expiry) =>
        log.debug("Access token fetched with expiry at {}", expiry)
        waiting.foreach { replyTo =>
          replyTo ! StatusReply.success(token)
        }
        context.become(caching(token, expiry))

      case GetFailed(throwable) =>
        log.debug("Access token failed to load", throwable)
        waiting.foreach { replyTo =>
          replyTo ! StatusReply.error(throwable)
        }
        context.become(idle)

      case msg =>
        log.debug("Unexpected message while loading: {}", msg)
    }

    private def caching(token: String, expiry: Instant): Receive = {
      case GetAccessToken if expiry.isAfter(Instant.now().plus(1, ChronoUnit.MINUTES)) =>
        sender() ! StatusReply.Success(token)
      case GetAccessToken =>
        log.debug("Access token expired at {}, fetching new one", expiry)
        fetchAccessToken()
        context.become(loading(Seq(sender())))
      case msg =>
        log.debug("Unexpected message while caching: {}", msg)
    }

    private def fetchAccessToken(): Unit = {
      authClient
        .createAccessToken()
        .addHeader("Authorization", s"Bearer $refreshToken")
        .setDeadline(requestTimeout)
        .invoke(CreateAccessTokenRequest())
        .onComplete {
          case Success(token) =>
            self ! AccessToken(
              token.token,
              token.expireTime
                .map(_.asJavaInstant)
                .getOrElse(Instant.now().plus(30, ChronoUnit.MINUTES)))
          case Failure(exception) =>
            self ! GetFailed(exception)
        }
    }
  }
}
