/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import kalix.protocol.entity.Command
import kalix.protocol.workflow_entity.WorkflowEntityInit

object WorkflowExceptions {

  final case class WorkflowException(
      workflowId: String,
      commandId: Long,
      commandName: String,
      message: String,
      cause: Option[Throwable])
      extends RuntimeException(message, cause.orNull) {
    def this(workflowId: String, commandId: Long, commandName: String, message: String) =
      this(workflowId, commandId, commandName, message, None)
  }

  object WorkflowException {

    def apply(message: String, cause: Option[Throwable]): WorkflowException =
      WorkflowException(workflowId = "", commandId = 0, commandName = "", message, cause)

    def apply(command: Command, message: String, cause: Option[Throwable]): WorkflowException =
      WorkflowException(command.entityId, command.id, command.name, message, cause)

  }

  object ProtocolException {
    def apply(message: String): WorkflowException =
      WorkflowException(workflowId = "", commandId = 0, commandName = "", "Protocol error: " + message, None)

    def apply(command: Command, message: String): WorkflowException =
      WorkflowException(command.entityId, command.id, command.name, "Protocol error: " + message, None)

    def apply(workflowId: String, message: String): WorkflowException =
      WorkflowException(workflowId, commandId = 0, commandName = "", "Protocol error: " + message, None)

    def apply(init: WorkflowEntityInit, message: String): WorkflowException =
      ProtocolException(init.entityId, message)
  }

  def failureMessageForLog(cause: Throwable): String = cause match {
    case WorkflowException(workflowId, commandId, commandName, _, _) =>
      val commandDescription = if (commandId != 0) s" for command [$commandName]" else ""
      val workflowDescription = if (workflowId.nonEmpty) s" [$workflowId]" else ""
      s"Terminating workflow$workflowDescription due to unexpected failure$commandDescription"
    case _ => "Terminating workflow due to unexpected failure"
  }
}
