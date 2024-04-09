/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.workflow

import java.util
import java.util.Collections

import kalix.javasdk.workflow.WorkflowOptions

case class WorkflowOptionsImpl(override val forwardHeaders: java.util.Set[String]) extends WorkflowOptions {

  /**
   * Ask Kalix to forward these headers from the incoming request as metadata headers for the incoming commands. By
   * default, no headers except "X-Server-Timing" are forwarded.
   */
  override def withForwardHeaders(headers: util.Set[String]): WorkflowOptions =
    copy(forwardHeaders = Collections.unmodifiableSet(new util.HashSet(headers)))

}

object WorkflowOptionsImpl {
  val defaults = new WorkflowOptionsImpl(Collections.emptySet())
}
