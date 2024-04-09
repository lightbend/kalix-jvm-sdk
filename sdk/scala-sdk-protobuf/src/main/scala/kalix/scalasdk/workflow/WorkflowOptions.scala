/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.workflow

import kalix.scalasdk.impl.ComponentOptions

/** Workflow options. */
trait WorkflowOptions extends ComponentOptions {

  def withForwardHeaders(headers: Set[String]): WorkflowOptions
}
object WorkflowOptions {
  val defaults: WorkflowOptions =
    WorkflowOptionsImpl(Set.empty)

  private[kalix] final case class WorkflowOptionsImpl(forwardHeaders: Set[String]) extends WorkflowOptions {

    override def withForwardHeaders(headers: Set[String]): WorkflowOptionsImpl =
      copy(forwardHeaders = headers)
  }
}
