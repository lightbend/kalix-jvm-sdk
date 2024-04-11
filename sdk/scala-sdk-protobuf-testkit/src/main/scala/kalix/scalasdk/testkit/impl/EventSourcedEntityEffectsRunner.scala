/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import kalix.scalasdk.Metadata
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.testkit.EventSourcedResult

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
