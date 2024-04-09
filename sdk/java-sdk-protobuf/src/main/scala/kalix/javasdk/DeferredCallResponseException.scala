/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk

import kalix.javasdk.StatusCode.ErrorCode

/** Exception used when a DeferredCall fails to wrap the origin error, plus the error code associated. */
case class DeferredCallResponseException(description: String, errorCode: ErrorCode, cause: Throwable)
    extends RuntimeException(cause)
