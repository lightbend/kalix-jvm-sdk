/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl

import com.akkaserverless.javasdk.{eventsourcedentity, valueentity}
import com.akkaserverless.protocol.component.Failure
import com.akkaserverless.protocol.entity.Command
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedInit
import com.akkaserverless.protocol.value_entity.ValueEntityInit

object EntityExceptions {

  final case class EntityException(entityId: String,
                                   commandId: Long,
                                   commandName: String,
                                   message: String,
                                   cause: Option[Throwable])
      extends RuntimeException(message, cause.orNull)

  object EntityException {
    def apply(message: String): EntityException =
      EntityException(entityId = "", commandId = 0, commandName = "", message, None)

    def apply(command: Command, message: String): EntityException =
      EntityException(command.entityId, command.id, command.name, message, None)

    def apply(command: Command, message: String, cause: Option[Throwable]): EntityException =
      EntityException(command.entityId, command.id, command.name, message, cause)

    def apply(context: valueentity.CommandContext[_], message: String): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, None)

    def apply(context: valueentity.CommandContext[_], message: String, cause: Option[Throwable]): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, cause)

    def apply(context: eventsourcedentity.CommandContext, message: String): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, None)

    def apply(context: eventsourcedentity.CommandContext, message: String, cause: Option[Throwable]): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message, cause)
  }

  object ProtocolException {
    def apply(message: String): EntityException =
      EntityException(entityId = "", commandId = 0, commandName = "", "Protocol error: " + message, None)

    def apply(command: Command, message: String): EntityException =
      EntityException(command.entityId, command.id, command.name, "Protocol error: " + message, None)

    def apply(init: ValueEntityInit, message: String): EntityException =
      EntityException(init.entityId, commandId = 0, commandName = "", "Protocol error: " + message, None)

    def apply(init: EventSourcedInit, message: String): EntityException =
      EntityException(init.entityId, commandId = 0, commandName = "", "Protocol error: " + message, None)
  }

  def failure(cause: Throwable): Failure = cause match {
    case e: EntityException => Failure(e.commandId, e.message)
    case e => Failure(description = "Unexpected failure: " + e.getMessage)
  }

  def failureMessage(cause: Throwable): String = cause match {
    case EntityException(entityId, commandId, commandName, _, _) =>
      val commandDescription = if (commandId != 0) s" for command [$commandName]" else ""
      val entityDescription = if (entityId.nonEmpty) s"entity [$entityId]" else "entity"
      s"Terminating $entityDescription due to unexpected failure$commandDescription"
    case _ => "Terminating entity due to unexpected failure"
  }
}
