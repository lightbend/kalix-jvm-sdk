/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl;

import kalix.javasdk.Metadata;
import kalix.javasdk.eventsourcedentity.CommandContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.testkit.EventSourcedResult;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import scala.jdk.javaapi.CollectionConverters;

/** Extended by generated code, not meant for user extension */
public abstract class EventSourcedEntityEffectsRunner<S, E> {

  private EventSourcedEntity<S, E> entity;
  private S _state;
  private List<E> events = new ArrayList();

  public EventSourcedEntityEffectsRunner(EventSourcedEntity<S, E> entity) {
    this.entity = entity;
    this._state = entity.emptyState();
  }

  /** @return The current state of the entity after applying the event */
  protected abstract S handleEvent(S state, E event);

  /** @return The current state of the entity */
  public S getState() {
    return _state;
  }

  /** @return All events emitted by command handlers of this entity up to now */
  public List<E> getAllEvents() {
    return events;
  }

  /**
   * creates a command context to run the commands, then creates an event context to run the events,
   * and finally, creates a command context to run the side effects. It cleans each context after
   * each run.
   *
   * @return the result of the side effects
   */
  protected <R> EventSourcedResult<R> interpretEffects(
      Supplier<EventSourcedEntity.Effect<R>> effect, Metadata metadata) {
    var commandContext = new TestKitEventSourcedEntityCommandContext(metadata);
    EventSourcedEntity.Effect<R> effectExecuted;
    try {
      entity._internalSetCommandContext(Optional.of(commandContext));
      entity._internalSetCurrentState(this._state);
      effectExecuted = effect.get();
      this.events.addAll(EventSourcedResultImpl.eventsOf(effectExecuted));
    } finally {
      entity._internalSetCommandContext(Optional.empty());
    }
    try {
      entity._internalSetEventContext(Optional.of(new TestKitEventSourcedEntityEventContext()));
      for (Object event : EventSourcedResultImpl.eventsOf(effectExecuted)) {
        this._state = handleEvent(this._state, (E) event);
        entity._internalSetCurrentState(this._state);
      }
    } finally {
      entity._internalSetEventContext(Optional.empty());
    }
    EventSourcedResult<R> result;
    try {
      entity._internalSetCommandContext(Optional.of(commandContext));
      var secondaryEffect = EventSourcedResultImpl.secondaryEffectOf(effectExecuted, _state);
      result = new EventSourcedResultImpl<>(effectExecuted, _state, secondaryEffect);
    } finally {
      entity._internalSetCommandContext(Optional.empty());
    }
    return result;
  }
}
