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
