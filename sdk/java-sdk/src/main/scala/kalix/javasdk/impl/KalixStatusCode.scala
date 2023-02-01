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

package kalix.javasdk.impl

import io.grpc.Status
import kalix.javasdk.StatusCode
import kalix.javasdk.StatusCode.HttpErrorCode

object KalixStatusCode {
  def toGrpcCode(httpCode: StatusCode): Status.Code = {
    httpCode match {
      case HttpErrorCode.BAD_REQUEST           => Status.Code.INVALID_ARGUMENT
      case HttpErrorCode.UNAUTHORIZED          => Status.Code.UNAUTHENTICATED
      case HttpErrorCode.FORBIDDEN             => Status.Code.PERMISSION_DENIED
      case HttpErrorCode.NOT_FOUND             => Status.Code.NOT_FOUND
      case HttpErrorCode.GATEWAY_TIMEOUT       => Status.Code.DEADLINE_EXCEEDED
      case HttpErrorCode.CONFLICT              => Status.Code.ALREADY_EXISTS
      case HttpErrorCode.TOO_MANY_REQUESTS     => Status.Code.RESOURCE_EXHAUSTED
      case HttpErrorCode.INTERNAL_SERVER_ERROR => Status.Code.INTERNAL
      case HttpErrorCode.SERVICE_UNAVAILABLE   => Status.Code.UNAVAILABLE
      case _                                    => Status.Code.INTERNAL
    }
  }
}
