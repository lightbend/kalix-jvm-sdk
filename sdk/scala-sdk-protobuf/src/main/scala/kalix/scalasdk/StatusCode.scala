/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
}
