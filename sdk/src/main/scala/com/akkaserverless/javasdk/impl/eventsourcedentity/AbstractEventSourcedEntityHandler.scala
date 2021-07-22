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

import com.akkaserverless.javasdk.ServiceCallFactory
import com.akkaserverless.javasdk.eventsourcedentity.CommandContext
import com.akkaserverless.javasdk.eventsourcedentity.EventContext
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityCreationContext
import com.akkaserverless.javasdk.impl.EntityExceptions.EntityException
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.AbstractEventSourcedEntityHandler.CommandResult
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.NoPrimaryEffect
import com.google.protobuf.{Any => JavaPBAny}

import java.util.Optional

object AbstractEventSourcedEntityHandler {
  case class CommandResult(events: Vector[Any],
                           secondaryEffect: SecondaryEffectImpl,
                           snapshot: Option[Any],
                           endSequenceNumber: Long)
}

/**
 * @tparam S the type of the managed state for the entity
 * Not for manual user extension or interaction
 */
abstract class AbstractEventSourcedEntityHandler[S, E <: EventSourcedEntityBase[S]](protected val entity: E) {

  private var state: Option[S] = None

  final protected def stateOrEmpty(): S = state match {
    case None =>
      val emptyState = entity.emptyState()
      require(emptyState != null, "Entity empty state is not allowed to be null")
      state = Some(emptyState)
      emptyState
    case Some(state) => state
  }

  final protected def setState(newState: S): Unit =
    state = Some(newState)

  // "public" api against the impl/testkit
  final def handleSnapshot(snapshot: S): Unit = setState(snapshot)
  final def handleEvent(event: Object, context: EventContext): Unit = {
    entity.setEventContext(Optional.of(context))
    try {
      val newState = handleEvent(stateOrEmpty(), event)
      setState(newState)
    } finally {
      entity.setEventContext(Optional.empty())
    }
  }
  final def handleCommand(commandName: String,
                          command: JavaPBAny,
                          context: CommandContext,
                          snapshotEvery: Int,
                          eventContextFactory: Long => EventContext): CommandResult = {
    val commandEffect = try {
      entity.setCommandContext(Optional.of(context))
      handleCommand(commandName, stateOrEmpty(), command).asInstanceOf[EventSourcedEntityEffectImpl[Any]]
    } finally {
      entity.setCommandContext(Optional.empty())
    }
    var currentSequence = context.sequenceNumber()
    commandEffect.primaryEffect match {
      case EmitEvents(events) =>
        var shouldSnapshot = false
        events.foreach { event =>
          try {
            entity.setEventContext(Optional.of(eventContextFactory(currentSequence)))
            val newState = handleEvent(stateOrEmpty(), event)
            setState(newState)
          } finally {
            entity.setEventContext(Optional.empty())
          }
          currentSequence += 1
          shouldSnapshot = shouldSnapshot || (snapshotEvery > 0 && currentSequence % snapshotEvery == 0)
        }
        // FIXME currently snapshotting final state after applying all events even if trigger was mid-event stream?
        val endState = stateOrEmpty()
        val snapshot =
          if (shouldSnapshot) Option(endState)
          else None
        CommandResult(events.toVector, commandEffect.secondaryEffect(endState), snapshot, currentSequence)
      case NoPrimaryEffect =>
        CommandResult(Vector.empty, commandEffect.secondaryEffect(stateOrEmpty()), None, context.sequenceNumber())
    }
  }

  protected def handleEvent(state: S, event: Any): S
  protected def handleCommand(commandName: String, state: S, command: JavaPBAny): EventSourcedEntityBase.Effect[_]

}
