package com.example.domain;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
import com.akkaserverless.javasdk.testkit.EventSourcedResult;
import com.akkaserverless.javasdk.testkit.impl.EventSourcedResultImpl;
import com.akkaserverless.javasdk.testkit.impl.TestKitEventSourcedEntityContext;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing Counter
 */
public final class CounterTestTok {

  private CounterDomain.CounterState state;
  private Counter entity;
  private List<Object> events = new ArrayList<Object>();

  /**
   * Create a testkit instance of Counter
   * @param entityFactory A function that creates a Counter based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   */
  public static CounterTestTok of(Function<EventSourcedEntityContext, Counter> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /**
   * Create a testkit instance of Counter with a specific entity id.
   */
  public static CounterTestTok of(String entityId, Function<EventSourcedEntityContext, Counter> entityFactory) {
    return new CounterTestTok(entityFactory.apply(new TestKitEventSourcedEntityContext(entityId)));
  }

  /** Construction is done through the static CounterTestTok.of-methods */
  private CounterTestTok(Counter entity) {
    this.state = entity.emptyState();
    this.entity = entity;
  }

  private CounterTestTok(Counter entity, CounterDomain.CounterState state) {
    this.state = state;
    this.entity = entity;
  }

  /**
   * @return The current state of the Counter under test
   */
  public CounterDomain.CounterState getState() {
    return state;
  }

  /**
   * @return All events that has been emitted by command handlers since the creation of this testkit.
   *         Individual sets of events from a single command handler invokation can be found in the
   *         Result from calling it.
   */
  public List<Object> getAllEvents() {
    return this.events;
  }

  private CounterDomain.CounterState handleEvent(CounterDomain.CounterState state, Object event) {
    if (event instanceof CounterDomain.ValueIncreased) {
      return entity.valueIncreased(state, (CounterDomain.ValueIncreased) event);
    } else if (event instanceof CounterDomain.ValueDecreased) {
      return entity.valueDecreased(state, (CounterDomain.ValueDecreased) event);
    } else if (event instanceof CounterDomain.ValueReset) {
      return entity.valueReset(state, (CounterDomain.ValueReset) event);
    } else {
      throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
    }
  }

  @SuppressWarnings("unchecked")
  private <Reply> EventSourcedResult<Reply> interpretEffects(EventSourcedEntity.Effect<Reply> effect) {
    List<Object> events = EventSourcedResultImpl.eventsOf(effect);
    this.events.addAll(events);
    for(Object e: events) {
      this.state = handleEvent(state,e);
    }
    return new EventSourcedResultImpl(effect, state);
  }

  public EventSourcedResult<Empty> increase(CounterApi.IncreaseValue command) {
    EventSourcedEntity.Effect<Empty> effect = entity.increase(state, command);
    return interpretEffects(effect);
  }

  public EventSourcedResult<Empty> decrease(CounterApi.DecreaseValue command) {
    EventSourcedEntity.Effect<Empty> effect = entity.decrease(state, command);
    return interpretEffects(effect);
  }

  public EventSourcedResult<Empty> reset(CounterApi.ResetValue command) {
    EventSourcedEntity.Effect<Empty> effect = entity.reset(state, command);
    return interpretEffects(effect);
  }

  public EventSourcedResult<CounterApi.CurrentCounter> getCurrentCounter(CounterApi.GetCounter command) {
    EventSourcedEntity.Effect<CounterApi.CurrentCounter> effect = entity.getCurrentCounter(state, command);
    return interpretEffects(effect);
  }
}
