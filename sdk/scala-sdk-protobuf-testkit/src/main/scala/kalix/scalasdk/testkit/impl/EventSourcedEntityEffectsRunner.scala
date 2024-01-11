/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.scalasdk.testkit.impl

import kalix.scalasdk.Metadata
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.testkit.EventSourcedResult
import kalix.scalasdk.testkit.impl.EventSourcedResultImpl
import scala.collection.immutable.Seq

import kalix.scalasdk.eventsourcedentity.CommandContext

/** Extended by generated code, not meant for user extension */
abstract class EventSourcedEntityEffectsRunner[S](entity: EventSourcedEntity[S]) {
  private var _state: S = entity.emptyState
  private var events: Seq[Any] = Nil

  protected def handleEvent(state: S, event: Any): S

  /** @return The current state of the entity */
  def currentState: S = _state

  /** @return All events emitted by command handlers of this entity up to now */
  def allEvents: Seq[Any] = events

  protected def interpretEffects[R](
      effect: () => EventSourcedEntity.Effect[R],
      metadata: Metadata = Metadata.empty): EventSourcedResult[R] = {
    val commandContext = new TestKitEventSourcedEntityCommandContext(metadata = metadata)
    val effectExecuted =
      try {
        entity._internalSetCommandContext(Some(commandContext))
        val effectExecuted = effect()
        this.events ++= EventSourcedResultImpl.eventsOf(effectExecuted)
        effectExecuted
      } finally {
        entity._internalSetCommandContext(None)
      }
    try {
      entity._internalSetEventContext(Some(new TestKitEventSourcedEntityEventContext()))
      this._state = EventSourcedResultImpl.eventsOf(effectExecuted).foldLeft(this._state)(handleEvent)
    } finally {
      entity._internalSetEventContext(None)
    }
    val result =
      try {
        entity._internalSetCommandContext(Some(commandContext))
        val secondaryEffect = EventSourcedResultImpl.secondaryEffectOf(effectExecuted, _state)
        new EventSourcedResultImpl[R, S](effectExecuted, _state, secondaryEffect)
      } finally {
        entity._internalSetCommandContext(None)
      }
    result
  }
}
