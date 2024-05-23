/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import kalix.javasdk.eventsourcedentity.{ CommandContext => ESECommandContext }
import kalix.javasdk.valueentity.{ CommandContext => VECommandContext }
import kalix.protocol.entity.Command
import kalix.protocol.event_sourced_entity.EventSourcedInit
import kalix.protocol.replicated_entity.ReplicatedEntityInit
import kalix.protocol.value_entity.ValueEntityInit

object EntityExceptions {

  final case class EntityException(
      entityId: String,
      commandId: Long,
      commandName: String,
      message: String,
      cause: Option[Throwable])
      extends RuntimeException(message, cause.orNull) {
    def this(entityId: String, commandId: Long, commandName: String, message: String) =
      this(entityId, commandId, commandName, message, None)
  }

  object EntityException {
    def apply(message: String): EntityException =
      EntityException(entityId = "", commandId = 0, commandName = "", message, None)

    def apply(message: String, cause: Option[Throwable]): EntityException =
      EntityException(entityId = "", commandId = 0, commandName = "", message, cause)

    def apply(command: Command, message: String): EntityException =
      EntityException(command.entityId, command.id, command.name, message, None)

    def apply(command: Command, message: String, cause: Option[Throwable]): EntityException =
      EntityException(command.entityId, command.id, command.name, message, cause)

    def apply(context: VECommandContext, message: String): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, None)

    def apply(context: VECommandContext, message: String, cause: Option[Throwable]): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, cause)

    def apply(context: ESECommandContext, message: String): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, None)

    def apply(context: ESECommandContext, message: String, cause: Option[Throwable]): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, cause)
  }

  object ProtocolException {
    def apply(message: String): EntityException =
      EntityException(entityId = "", commandId = 0, commandName = "", "Protocol error: " + message, None)

    def apply(command: Command, message: String): EntityException =
      EntityException(command.entityId, command.id, command.name, "Protocol error: " + message, None)

    def apply(entityId: String, message: String): EntityException =
      EntityException(entityId, commandId = 0, commandName = "", "Protocol error: " + message, None)

    def apply(init: ValueEntityInit, message: String): EntityException =
      ProtocolException(init.entityId, message)

    def apply(init: EventSourcedInit, message: String): EntityException =
      ProtocolException(init.entityId, message)

    def apply(init: ReplicatedEntityInit, message: String): EntityException =
      ProtocolException(init.entityId, message)

  }

  def failureMessageForLog(cause: Throwable): String = cause match {
    case EntityException(entityId, commandId, commandName, _, _) =>
      val commandDescription = if (commandId != 0) s" for command [$commandName]" else ""
      val entityDescription = if (entityId.nonEmpty) s" [$entityId]" else ""
      s"Terminating entity$entityDescription due to unexpected failure$commandDescription"
    case _ => "Terminating entity due to unexpected failure"
  }
}
