/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.eventsourcedentity

import kalix.javasdk.eventsourcedentity.CommandContext
import kalix.javasdk.eventsourcedentity.EventContext
import kalix.javasdk.eventsourcedentity.EventSourcedEntity
import kalix.javasdk.impl.EntityExceptions
import kalix.javasdk.impl.effect.SecondaryEffectImpl
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.NoPrimaryEffect

import java.util.Optional

object EventSourcedEntityRouter {

  final case class CommandResult(
      events: Vector[Any],
      secondaryEffect: SecondaryEffectImpl,
      snapshot: Option[Any],
      endSequenceNumber: Long,
      deleteEntity: Boolean)

  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException

  final case class EventHandlerNotFound(eventClass: Class[_]) extends RuntimeException
}

/**
 * @tparam S
 *   the type of the managed state for the entity Not for manual user extension or interaction
 *
 * The concrete <code>EventSourcedEntityRouter</code> is generated for the specific entities defined in Protobuf.
 */
abstract class EventSourcedEntityRouter[S, E, ES <: EventSourcedEntity[S, E]](protected val entity: ES) {
  import EventSourcedEntityRouter._

  private var state: Option[S] = None

  /** INTERNAL API */
  // "public" api against the impl/testkit
  def _stateOrEmpty(): S = state match {
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
  final def _internalHandleEvent(event: E, context: EventContext): Unit = {
    entity._internalSetEventContext(Optional.of(context))
    try {
      val newState = handleEvent(_stateOrEmpty(), event)
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
        entity._internalSetCurrentState(_stateOrEmpty())
        handleCommand(commandName, _stateOrEmpty(), command, context).asInstanceOf[EventSourcedEntityEffectImpl[Any, E]]
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
      case EmitEvents(events, deleteEntity) =>
        var shouldSnapshot = false
        events.foreach { event =>
          try {
            entity._internalSetEventContext(Optional.of(eventContextFactory(currentSequence)))
            val newState = handleEvent(_stateOrEmpty(), event.asInstanceOf[E])
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
        val endState = _stateOrEmpty()
        val snapshot =
          if (shouldSnapshot) Option(endState)
          else None

        try {
          // side effect callbacks may want to access context or components which is valid
          entity._internalSetCommandContext(Optional.of(context))
          CommandResult(
            events.toVector,
            commandEffect.secondaryEffect(endState),
            snapshot,
            currentSequence,
            deleteEntity)
        } finally {
          entity._internalSetCommandContext(Optional.empty())
        }
      case NoPrimaryEffect =>
        try {
          // side effect callbacks may want to access context or components which is valid
          entity._internalSetCommandContext(Optional.of(context))
          CommandResult(
            Vector.empty,
            commandEffect.secondaryEffect(_stateOrEmpty()),
            None,
            context.sequenceNumber(),
            deleteEntity = false)
        } finally {
          entity._internalSetCommandContext(Optional.empty())
        }
    }
  }

  def handleEvent(state: S, event: E): S

  def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): EventSourcedEntity.Effect[_]

  def entityClass: Class[_] = entity.getClass
}
