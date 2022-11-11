package org.example.valueentity.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.Metadata;
import kalix.javasdk.impl.effect.MessageReplyImpl;
import kalix.javasdk.impl.effect.SecondaryEffectImpl;
import kalix.javasdk.impl.valueentity.ValueEntityEffectImpl;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.testkit.impl.TestKitValueEntityCommandContext;
import kalix.javasdk.testkit.impl.TestKitValueEntityContext;
import kalix.javasdk.testkit.impl.ValueEntityResultImpl;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import org.example.valueentity.CounterApi;
import org.example.valueentity.state.OuterCounterState;

import java.util.Optional;
import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing Counter
 */
public final class CounterTestKit {

  private OuterCounterState.CounterState state;
  private final OuterCounterState.CounterState emptyState;
  private final Counter entity;
  private final String entityId;

  /**
   * Create a testkit instance of Counter
   * @param entityFactory A function that creates a Counter based on the given ValueEntityContext,
   *                      a default entity id is used.
   */
  public static CounterTestKit of(Function<ValueEntityContext, Counter> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /**
   * Create a testkit instance of Counter with a specific entity id.
   */
  public static CounterTestKit of(String entityId, Function<ValueEntityContext, Counter> entityFactory) {
    return new CounterTestKit(entityFactory.apply(new TestKitValueEntityContext(entityId)), entityId);
  }

  /** Construction is done through the static CounterTestKit.of-methods */
  private CounterTestKit(Counter entity, String entityId) {
    this.entityId = entityId;
    this.state = entity.emptyState();
    this.emptyState = state;
    this.entity = entity;
  }

  private CounterTestKit(Counter entity, String entityId, OuterCounterState.CounterState state) {
    this.entityId = entityId;
    this.state = state;
    this.emptyState = state;
    this.entity = entity;
  }

  /**
   * @return The current state of the Counter under test
   */
  public OuterCounterState.CounterState getState() {
    return state;
  }

  private <Reply> ValueEntityResult<Reply> interpretEffects(ValueEntity.Effect<Reply> effect) {
    @SuppressWarnings("unchecked")
    ValueEntityResultImpl<Reply> result = new ValueEntityResultImpl<>(effect);
    if (result.stateWasUpdated()) {
      this.state = (OuterCounterState.CounterState) result.getUpdatedState();
    } else if (result.stateWasDeleted()) {
      this.state = emptyState;
    }
    return result;
  }

  public ValueEntityResult<Empty> increase(CounterApi.IncreaseValue increaseValue, Metadata metadata) {
    entity._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, metadata)));
    entity._internalSetCurrentState(state);
    ValueEntity.Effect<Empty> effect = entity.increase(state, increaseValue);
    return interpretEffects(effect);
  }

  public ValueEntityResult<Empty> decrease(CounterApi.DecreaseValue decreaseValue, Metadata metadata) {
    entity._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, metadata)));
    entity._internalSetCurrentState(state);
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
