/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl

import com.akkaserverless.javasdk.{entity, eventsourced}
import com.akkaserverless.protocol.entity.{Command, Failure}
import com.akkaserverless.protocol.event_sourced.EventSourcedInit
import com.akkaserverless.protocol.value_entity.ValueEntityInit

object EntityExceptions {

  final case class EntityException(entityId: String, commandId: Long, commandName: String, message: String)
      extends RuntimeException(message)

  object EntityException {
    def apply(message: String): EntityException =
      EntityException(entityId = "", commandId = 0, commandName = "", message)

    def apply(command: Command, message: String): EntityException =
      EntityException(command.entityId, command.id, command.name, message)

    def apply(context: entity.CommandContext[_], message: String): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message)

    def apply(context: eventsourced.CommandContext, message: String): EntityException =
      EntityException(context.entityId, context.commandId, context.commandName, message)
  }

  object ProtocolException {
    def apply(message: String): EntityException =
      EntityException(entityId = "", commandId = 0, commandName = "", "Protocol error: " + message)

    def apply(command: Command, message: String): EntityException =
      EntityException(command.entityId, command.id, command.name, "Protocol error: " + message)

    def apply(init: ValueEntityInit, message: String): EntityException =
      EntityException(init.entityId, commandId = 0, commandName = "", "Protocol error: " + message)

    def apply(init: EventSourcedInit, message: String): EntityException =
      EntityException(init.entityId, commandId = 0, commandName = "", "Protocol error: " + message)
  }

  def failure(cause: Throwable): Failure = cause match {
    case e: EntityException => Failure(e.commandId, e.message)
    case e => Failure(description = "Unexpected failure: " + e.getMessage)
  }

  def failureMessage(cause: Throwable): String = cause match {
    case EntityException(entityId, commandId, commandName, _) =>
      val commandDescription = if (commandId != 0) s" for command [$commandName]" else ""
      val entityDescription = if (entityId.nonEmpty) s"entity [$entityId]" else "entity"
      s"Terminating $entityDescription due to unexpected failure$commandDescription"
    case _ => "Terminating entity due to unexpected failure"
  }
}
