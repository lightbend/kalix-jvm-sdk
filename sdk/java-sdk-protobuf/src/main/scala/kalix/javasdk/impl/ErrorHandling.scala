/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import org.slf4j.MDC

import java.util.UUID

/**
 * INTERNAL API
 */
private[javasdk] object ErrorHandling {

  case class BadRequestException(msg: String) extends RuntimeException(msg)

  val CorrelationIdMdcKey = "correlationID"

  def withCorrelationId[T](block: String => T): T = {
    val correlationId = UUID.randomUUID().toString
    MDC.put(CorrelationIdMdcKey, correlationId)
    try {
      block(correlationId)
    } finally {
      MDC.remove(CorrelationIdMdcKey)
    }
  }

}
