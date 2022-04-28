package org.example.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.impl.effect.MessageReplyImpl;
import kalix.javasdk.impl.effect.SecondaryEffectImpl;
import kalix.javasdk.impl.valueentity.ValueEntityEffectImpl;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.testkit.impl.TestKitValueEntityContext;
import kalix.javasdk.testkit.impl.ValueEntityResultImpl;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import org.example.state.OuterCounterState;
import org.example.valueentity.CounterApi;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing Counter
 */
public final class CounterTestKit {

  private OuterCounterState.CounterState state;
  private Counter entity;

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
    return new CounterTestKit(entityFactory.apply(new TestKitValueEntityContext(entityId)));
  }

  /** Construction is done through the static CounterTestKit.of-methods */
  private CounterTestKit(Counter entity) {
    this.state = entity.emptyState();
    this.entity = entity;
  }

  private CounterTestKit(Counter entity, OuterCounterState.CounterState state) {
    this.state = state;
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
    }
    return result;
  }

  public ValueEntityResult<Empty> increase(CounterApi.IncreaseValue increaseValue) {
    ValueEntity.Effect<Empty> effect = entity.increase(state, increaseValue);
    return interpretEffects(effect);
  }

  public ValueEntityResult<Empty> decrease(CounterApi.DecreaseValue decreaseValue) {
    ValueEntity.Effect<Empty> effect = entity.decrease(state, decreaseValue);
    return interpretEffects(effect);
  }
}
