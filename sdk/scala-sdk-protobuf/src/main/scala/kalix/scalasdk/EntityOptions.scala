/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import kalix.scalasdk.impl.ComponentOptions

/** Options used for configuring an entity. */
trait EntityOptions extends ComponentOptions {

  /**
   * @return
   *   the headers requested to be forwarded as metadata (cannot be mutated, use withForwardHeaders)
   */
  def forwardHeaders: Set[String]

  /**
   * Ask Kalix to forward these headers from the incoming request as metadata headers for the incoming commands. By
   * default no headers except "X-Server-Timing" are forwarded.
   */
  def withForwardHeaders(headers: Set[String]): EntityOptions
}
