/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import io.grpc.Status
import kalix.javasdk.StatusCode
import kalix.javasdk.StatusCode.{ ErrorCode => JErrorCode }

object StatusCodeConverter {
  def toGrpcCode(statusCode: StatusCode): Status.Code = {
    statusCode match {
      case JErrorCode.BAD_REQUEST           => Status.Code.INVALID_ARGUMENT
      case JErrorCode.UNAUTHORIZED          => Status.Code.UNAUTHENTICATED
      case JErrorCode.FORBIDDEN             => Status.Code.PERMISSION_DENIED
      case JErrorCode.NOT_FOUND             => Status.Code.NOT_FOUND
      case JErrorCode.GATEWAY_TIMEOUT       => Status.Code.DEADLINE_EXCEEDED
      case JErrorCode.CONFLICT              => Status.Code.ALREADY_EXISTS
      case JErrorCode.TOO_MANY_REQUESTS     => Status.Code.RESOURCE_EXHAUSTED
      case JErrorCode.INTERNAL_SERVER_ERROR => Status.Code.INTERNAL
      case JErrorCode.SERVICE_UNAVAILABLE   => Status.Code.UNAVAILABLE
      case _                                => Status.Code.INTERNAL
    }
  }

  def fromGrpcCode(statusCode: Status.Code): JErrorCode = {
    statusCode match {
      case Status.Code.INVALID_ARGUMENT   => JErrorCode.BAD_REQUEST
      case Status.Code.UNAUTHENTICATED    => JErrorCode.UNAUTHORIZED
      case Status.Code.PERMISSION_DENIED  => JErrorCode.FORBIDDEN
      case Status.Code.NOT_FOUND          => JErrorCode.NOT_FOUND
      case Status.Code.DEADLINE_EXCEEDED  => JErrorCode.GATEWAY_TIMEOUT
      case Status.Code.ALREADY_EXISTS     => JErrorCode.CONFLICT
      case Status.Code.RESOURCE_EXHAUSTED => JErrorCode.TOO_MANY_REQUESTS
      case Status.Code.INTERNAL           => JErrorCode.INTERNAL_SERVER_ERROR
      case Status.Code.UNAVAILABLE        => JErrorCode.SERVICE_UNAVAILABLE
      case _                              => JErrorCode.INTERNAL_SERVER_ERROR
    }
  }

}
