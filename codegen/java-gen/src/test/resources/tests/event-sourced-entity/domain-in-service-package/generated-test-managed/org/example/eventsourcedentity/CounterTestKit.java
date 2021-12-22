package org.example.eventsourcedentity;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
import com.akkaserverless.javasdk.testkit.EventSourcedResult;
import com.akkaserverless.javasdk.testkit.impl.EventSourcedEntityEffectsRunner;
import com.akkaserverless.javasdk.testkit.impl.EventSourcedResultImpl;
import com.akkaserverless.javasdk.testkit.impl.TestKitEventSourcedEntityCommandContext;
import com.akkaserverless.javasdk.testkit.impl.TestKitEventSourcedEntityContext;
import com.akkaserverless.javasdk.testkit.impl.TestKitEventSourcedEntityEventContext;
import com.google.protobuf.Empty;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing Counter
 */
public final class CounterTestKit extends EventSourcedEntityEffectsRunner<CounterDomain.CounterState> {

  /**
   * Create a testkit instance of Counter
   * @param entityFactory A function that creates a Counter based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   */
  public static CounterTestKit of(Function<EventSourcedEntityContext, Counter> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /**
   * Create a testkit instance of Counter with a specific entity id.
   */
  public static CounterTestKit of(String entityId, Function<EventSourcedEntityContext, Counter> entityFactory) {
    return new CounterTestKit(entityFactory.apply(new TestKitEventSourcedEntityContext(entityId)));
  }

  private Counter entity;

  /** Construction is done through the static CounterTestKit.of-methods */
  private CounterTestKit(Counter entity) {
     super(entity);
     this.entity = entity;
  }

  public CounterDomain.CounterState handleEvent(CounterDomain.CounterState state, Object event) {
    try {
      entity._internalSetEventContext(Optional.of(new TestKitEventSourcedEntityEventContext()));
      if (event instanceof CounterDomain.Increased) {
        return entity.increased(state, (CounterDomain.Increased) event);
      } else if (event instanceof CounterDomain.Decreased) {
        return entity.decreased(state, (CounterDomain.Decreased) event);
      } else {
        throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
      }
    } finally {
      entity._internalSetEventContext(Optional.empty());
    }
  }

  public EventSourcedResult<Empty> increase(CounterApi.IncreaseValue command) {
    return interpretEffects(() -> entity.increase(getState(), command));
  }

  public EventSourcedResult<Empty> decrease(CounterApi.DecreaseValue command) {
    return interpretEffects(() -> entity.decrease(getState(), command));
  }
}
