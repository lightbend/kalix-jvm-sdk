/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.workflow

import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.CommandContext

/**
 * INTERNAL API, but used by generated code.
 */
abstract class WorkflowRouter[S >: Null, E <: AbstractWorkflow[S]](val workflow: E) {
  def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): AbstractWorkflow.Effect[_]
}
