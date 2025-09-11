/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

/**
 * A sealed trait representing HTTP status codes.
 */
sealed trait StatusCode

/**
 * Companion object for the `StatusCode` trait, containing various HTTP status code definitions.
 */
object StatusCode {
  sealed abstract class Success(val value: Int) extends StatusCode

  case object Ok extends Success(200)

  case object Created extends Success(201)

  case object Accepted extends Success(202)

  case object NonAuthoritativeInformation extends Success(203)

  case object NoContent extends Success(204)

  case object ResetContent extends Success(205)

  case object PartialContent extends Success(206)

  case object MultiStatus extends Success(207)

  case object AlreadyReported extends Success(208)

  case object IMUsed extends Success(226)

  sealed abstract class Redirect(val value: Int) extends StatusCode

  case object MultipleChoices extends Redirect(300)

  case object MovedPermanently extends Redirect(301)

  case object Found extends Redirect(302)

  case object SeeOther extends Redirect(303)

  case object NotModified extends Redirect(304)

  case object UseProxy extends Redirect(305)

  case object TemporaryRedirect extends Redirect(307)

  case object PermanentRedirect extends Redirect(308)

  sealed abstract class Error(val value: Int) extends StatusCode

  case object BadRequest extends Error(400)

  case object Unauthorized extends Error(401)

  case object Forbidden extends Error(403)

  case object NotFound extends Error(404)

  case object Conflict extends Error(409)

  case object TooManyRequests extends Error(429)

  case object InternalServerError extends Error(500)

  case object ServiceUnavailable extends Error(503)

  case object GatewayTimeout extends Error(504)

}
