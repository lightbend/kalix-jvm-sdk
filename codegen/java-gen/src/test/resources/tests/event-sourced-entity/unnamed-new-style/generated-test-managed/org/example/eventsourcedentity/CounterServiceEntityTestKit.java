package org.example.eventsourcedentity;

import com.google.protobuf.Empty;
import kalix.javasdk.Metadata;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.impl.effect.MessageReplyImpl;
import kalix.javasdk.impl.effect.SecondaryEffectImpl;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.impl.EventSourcedEntityEffectsRunner;
import kalix.javasdk.testkit.impl.EventSourcedResultImpl;
import kalix.javasdk.testkit.impl.TestKitEventSourcedEntityCommandContext;
import kalix.javasdk.testkit.impl.TestKitEventSourcedEntityContext;
import kalix.javasdk.testkit.impl.TestKitEventSourcedEntityEventContext;
import org.example.eventsourcedentity.domain.CounterDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing CounterServiceEntity
 */
public final class CounterServiceEntityTestKit extends EventSourcedEntityEffectsRunner<CounterDomain.CounterState, Object> {

  /**
   * Create a testkit instance of CounterServiceEntity
   * @param entityFactory A function that creates a CounterServiceEntity based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   */
  public static CounterServiceEntityTestKit of(Function<EventSourcedEntityContext, CounterServiceEntity> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /**
   * Create a testkit instance of CounterServiceEntity with a specific entity id.
   */
  public static CounterServiceEntityTestKit of(String entityId, Function<EventSourcedEntityContext, CounterServiceEntity> entityFactory) {
    return new CounterServiceEntityTestKit(entityFactory.apply(new TestKitEventSourcedEntityContext(entityId)));
  }

  private CounterServiceEntity entity;

  /** Construction is done through the static CounterServiceEntityTestKit.of-methods */
  private CounterServiceEntityTestKit(CounterServiceEntity entity) {
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

  public EventSourcedResult<Empty> increase(CounterApi.IncreaseValue command, Metadata metadata) {
    return interpretEffects(() -> entity.increase(getState(), command), metadata);
  }

  public EventSourcedResult<Empty> decrease(CounterApi.DecreaseValue command, Metadata metadata) {
    return interpretEffects(() -> entity.decrease(getState(), command), metadata);
  }

  public EventSourcedResult<Empty> increase(CounterApi.IncreaseValue command) {
    return interpretEffects(() -> entity.increase(getState(), command), Metadata.EMPTY);
  }

  public EventSourcedResult<Empty> decrease(CounterApi.DecreaseValue command) {
    return interpretEffects(() -> entity.decrease(getState(), command), Metadata.EMPTY);
  }

}
