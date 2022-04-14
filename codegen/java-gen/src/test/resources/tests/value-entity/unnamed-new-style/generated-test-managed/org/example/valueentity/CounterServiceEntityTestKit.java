package org.example.valueentity;

import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl;
import com.akkaserverless.javasdk.testkit.ValueEntityResult;
import com.akkaserverless.javasdk.testkit.impl.TestKitValueEntityCommandContext;
import com.akkaserverless.javasdk.testkit.impl.TestKitValueEntityContext;
import com.akkaserverless.javasdk.testkit.impl.ValueEntityResultImpl;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.google.protobuf.Empty;
import org.example.valueentity.domain.CounterDomain;

import java.util.Optional;
import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing CounterServiceEntity
 */
public final class CounterServiceEntityTestKit {

  private CounterDomain.CounterState state;
  private CounterServiceEntity entity;
  private String entityId;

  /**
   * Create a testkit instance of CounterServiceEntity
   * @param entityFactory A function that creates a CounterServiceEntity based on the given ValueEntityContext,
   *                      a default entity id is used.
   */
  public static CounterServiceEntityTestKit of(Function<ValueEntityContext, CounterServiceEntity> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /**
   * Create a testkit instance of CounterServiceEntity with a specific entity id.
   */
  public static CounterServiceEntityTestKit of(String entityId, Function<ValueEntityContext, CounterServiceEntity> entityFactory) {
    return new CounterServiceEntityTestKit(entityFactory.apply(new TestKitValueEntityContext(entityId)), entityId);
  }

  /** Construction is done through the static CounterServiceEntityTestKit.of-methods */
  private CounterServiceEntityTestKit(CounterServiceEntity entity, String entityId) {
    this.entityId = entityId;
    this.state = entity.emptyState();
    this.entity = entity;
  }

  private CounterServiceEntityTestKit(CounterServiceEntity entity, String entityId, CounterDomain.CounterState state) {
    this.entityId = entityId;
    this.state = state;
    this.entity = entity;
  }

  /**
   * @return The current state of the CounterServiceEntity under test
   */
  public CounterDomain.CounterState getState() {
    return state;
  }

  private <Reply> ValueEntityResult<Reply> interpretEffects(ValueEntity.Effect<Reply> effect) {
    @SuppressWarnings("unchecked")
    ValueEntityResultImpl<Reply> result = new ValueEntityResultImpl<>(effect);
    if (result.stateWasUpdated()) {
      this.state = (CounterDomain.CounterState) result.getUpdatedState();
    }
    return result;
  }

  public ValueEntityResult<Empty> increase(CounterApi.IncreaseValue increaseValue, Metadata metadata) {
    entity ._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, metadata)));
    ValueEntity.Effect<Empty> effect = entity.increase(state, increaseValue);
    return interpretEffects(effect);
  }

  public ValueEntityResult<Empty> decrease(CounterApi.DecreaseValue decreaseValue, Metadata metadata) {
    entity ._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, metadata)));
    ValueEntity.Effect<Empty> effect = entity.decrease(state, decreaseValue);
    return interpretEffects(effect);
  }

  public ValueEntityResult<Empty> increase(CounterApi.IncreaseValue increaseValue) {
    entity ._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, Metadata.EMPTY)));
    ValueEntity.Effect<Empty> effect = entity.increase(state, increaseValue);
    return interpretEffects(effect);
  }

  public ValueEntityResult<Empty> decrease(CounterApi.DecreaseValue decreaseValue) {
    entity ._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, Metadata.EMPTY)));
    ValueEntity.Effect<Empty> effect = entity.decrease(state, decreaseValue);
    return interpretEffects(effect);
  }
}
