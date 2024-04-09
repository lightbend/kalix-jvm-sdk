/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.action;

import kalix.javasdk.impl.ComponentOptions;
import kalix.javasdk.impl.action.ActionOptionsImpl;

import java.util.Collections;

/** Options for actions */
public interface ActionOptions extends ComponentOptions {

  /** Create default options for an action. */
  static ActionOptions defaults() {
    return new ActionOptionsImpl(Collections.emptySet());
  }

  /**
   * @return the headers requested to be forwarded as metadata (cannot be mutated, use
   *     withForwardHeaders)
   */
  java.util.Set<String> forwardHeaders();

  /**
   * Ask Kalix to forward these headers from the incoming request as metadata headers for the
   * incoming commands. By default, no headers except "X-Server-Timing" are forwarded.
   */
  ActionOptions withForwardHeaders(java.util.Set<String> headers);
}
