/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import io.opentelemetry.context.{ Context => OtelContext }

trait TraceContext {

  /**
   * Allows retrieving the trace context as an OpenTelemetry context for easier construction of child spans. If the
   * trace context is not available, a new empty context will be returned.
   *
   * @return
   *   the trace context as an OpenTelemetry context.
   */
  def asOpenTelemetryContext: OtelContext

  /**
   * Allows retrieving the trace parent for easier injection in external calls (e.g. HTTP request headers).
   *
   * @return
   *   the trace parent using W3C Trace Context format.
   * @see
   *   <a href="https://www.w3.org/TR/trace-context/#trace-context-http-headers-format">W3C Trace Context section 3</a>
   */
  def traceParent: Option[String]

  /**
   * Allows retrieving the trace state for easier injection in external calls (e.g. HTTP request headers).
   *
   * @return
   *   the trace state using W3C Trace Context format.
   * @see
   *   <a href="https://www.w3.org/TR/trace-context/#trace-context-http-headers-format">W3C Trace Context section 3</a>
   */
  def traceState: Option[String]
}
