/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.javasdk.impl.eventsourcedentity

import java.util.Optional

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext
import com.akkaserverless.javasdk.eventsourcedentity.EventContext
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.javasdk.impl.EntityExceptions
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.NoPrimaryEffect

object EventSourcedEntityHandler {
  final case class CommandResult(
      events: Vector[Any],
      secondaryEffect: SecondaryEffectImpl,
      snapshot: Option[Any],
      endSequenceNumber: Long)

  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException

  final case class EventHandlerNotFound(eventClass: Class[_]) extends RuntimeException
}

/**
 * @tparam S
 *   the type of the managed state for the entity Not for manual user extension or interaction
 *
 * The concrete <code>EventSourcedEntityHandler</code> is generated for the specific entities defined in Protobuf.
 */
abstract class EventSourcedEntityHandler[S, E <: EventSourcedEntity[S]](protected val entity: E) {
  import EventSourcedEntityHandler._

  private var state: Option[S] = None

  private def stateOrEmpty(): S = state match {
    case None =>
      val emptyState = entity.emptyState()
      // null is allowed as emptyState
      state = Some(emptyState)
      emptyState
    case Some(state) => state
  }

  private def setState(newState: S): Unit =
    state = Option(newState)

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHandleSnapshot(snapshot: S): Unit = setState(snapshot)

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHandleEvent(event: Object, context: EventContext): Unit = {
    entity._internalSetEventContext(Optional.of(context))
    try {
      val newState = handleEvent(stateOrEmpty(), event)
      setState(newState)
    } catch {
      case EventHandlerNotFound(eventClass) =>
        throw new IllegalArgumentException(s"Unknown event type [$eventClass] on ${entity.getClass}")
    } finally {
      entity._internalSetEventContext(Optional.empty())
    }
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHandleCommand(
      commandName: String,
      command: Any,
      context: CommandContext,
      snapshotEvery: Int,
      eventContextFactory: Long => EventContext): CommandResult = {
    val commandEffect =
      try {
        entity._internalSetCommandContext(Optional.of(context))
        handleCommand(commandName, stateOrEmpty(), command, context).asInstanceOf[EventSourcedEntityEffectImpl[Any]]
      } catch {
        case CommandHandlerNotFound(name) =>
          throw new EntityExceptions.EntityException(
            context.entityId(),
            context.commandId(),
            commandName,
            s"No command handler found for command [$name] on ${entity.getClass}")
      } finally {
        entity._internalSetCommandContext(Optional.empty())
      }
    var currentSequence = context.sequenceNumber()
    commandEffect.primaryEffect match {
      case EmitEvents(events) =>
        var shouldSnapshot = false
        events.foreach { event =>
          try {
            entity._internalSetEventContext(Optional.of(eventContextFactory(currentSequence)))
            val newState = handleEvent(stateOrEmpty(), event)
            if (newState == null)
              throw new IllegalArgumentException("Event handler must not return null as the updated state.")
            setState(newState)
          } catch {
            case EventHandlerNotFound(eventClass) =>
              throw new IllegalArgumentException(s"Unknown event type [$eventClass] on ${entity.getClass}")
          } finally {
            entity._internalSetEventContext(Optional.empty())
          }
          currentSequence += 1
          shouldSnapshot = shouldSnapshot || (snapshotEvery > 0 && currentSequence % snapshotEvery == 0)
        }
        // snapshotting final state since that is the "atomic" write
        // emptyState can be null but null snapshot should not be stored, but that can't even
        // happen since event handler is not allowed to return null as newState
        val endState = stateOrEmpty()
        val snapshot =
          if (shouldSnapshot) Option(endState)
          else None
        CommandResult(events.toVector, commandEffect.secondaryEffect(endState), snapshot, currentSequence)
      case NoPrimaryEffect =>
        CommandResult(Vector.empty, commandEffect.secondaryEffect(stateOrEmpty()), None, context.sequenceNumber())
    }
  }

  def handleEvent(state: S, event: Any): S

  def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): EventSourcedEntity.Effect[_]

  def entityClass: Class[_] = entity.getClass
}
