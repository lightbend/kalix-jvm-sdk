/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import kalix.javasdk.impl.ComponentOptions;
/** Options used for configuring an entity. */
public interface EntityOptions extends ComponentOptions {


  /**
   * @return the headers requested to be forwarded as metadata (cannot be mutated, use
   *     withForwardHeaders)
   */
  java.util.Set<String> forwardHeaders();

  /**
   * Ask Kalix to forward these headers from the incoming request as metadata headers for the
   * incoming commands. By default, no headers except "X-Server-Timing" are forwarded.
   */
  ComponentOptions withForwardHeaders(java.util.Set<String> headers);
}
