/*
 * Copyright 2021 Lightbend Inc.
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

import kalix.javasdk.StatusCode.{ ErrorCode => JErrorCode }
import kalix.javasdk.{ StatusCode => JStatusCode }

sealed trait ErrorCode extends JStatusCode {}
object BadRequest extends ErrorCode
object Unauthorized extends ErrorCode
object Forbidden extends ErrorCode
object NotFound extends ErrorCode
object Conflict extends ErrorCode
object TooManyRequests extends ErrorCode
object InternalServerError extends ErrorCode
object ServiceUnavailable extends ErrorCode
object GatewayTimeout extends ErrorCode

private[scalasdk] object StatusCodeConverters {
  def toJava(statusCode: ErrorCode): JErrorCode = {
    statusCode match {
      case BadRequest          => JErrorCode.BAD_REQUEST
      case Unauthorized        => JErrorCode.UNAUTHORIZED
      case Forbidden           => JErrorCode.FORBIDDEN
      case NotFound            => JErrorCode.NOT_FOUND
      case Conflict            => JErrorCode.CONFLICT
      case TooManyRequests     => JErrorCode.TOO_MANY_REQUESTS
      case InternalServerError => JErrorCode.INTERNAL_SERVER_ERROR
      case ServiceUnavailable  => JErrorCode.SERVICE_UNAVAILABLE
      case GatewayTimeout      => JErrorCode.GATEWAY_TIMEOUT
    }
  }
}
