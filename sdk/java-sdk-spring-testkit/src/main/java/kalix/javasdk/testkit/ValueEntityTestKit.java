/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit;

import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.impl.TestKitValueEntityCommandContext;
import kalix.javasdk.testkit.impl.TestKitValueEntityContext;
import kalix.javasdk.testkit.impl.ValueEntityResultImpl;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ValueEntity Testkit for use in unit tests for Value entities.
 *
 * <p>To test a ValueEntity create a testkit instance by calling one of the available {@code
 * ValueEntityTestKit.of} methods. The returned testkit is stateful, and it holds internally the
 * state of the entity.
 *
 * <p>Use the {@code call} methods to interact with the testkit.
 */
public class ValueEntityTestKit<S, E extends ValueEntity<S>> {

  private S state;
  private final S emptyState;
  private final E entity;
  private final String entityId;

  private ValueEntityTestKit(String entityId, E entity) {
    this.entityId = entityId;
    this.entity = entity;
    this.state = entity.emptyState();
    this.emptyState = state;
  }

  /**
   * Creates a new testkit instance from a ValueEntity Supplier.
   *
   * <p>A default test entity id will be automatically provided.
   */
  public static <S, E extends ValueEntity<S>> ValueEntityTestKit<S, E> of(
      Supplier<E> entityFactory) {
    return of(ctx -> entityFactory.get());
  }

  /**
   * Creates a new testkit instance from a function ValueEntityContext to ValueEntity.
   *
   * <p>A default test entity id will be automatically provided.
   */
  public static <S, E extends ValueEntity<S>> ValueEntityTestKit<S, E> of(
      Function<ValueEntityContext, E> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /** Creates a new testkit instance from a user defined entity id and a ValueEntity Supplier. */
  public static <S, E extends ValueEntity<S>> ValueEntityTestKit<S, E> of(
      String entityId, Supplier<E> entityFactory) {
    return of(entityId, ctx -> entityFactory.get());
  }

  /**
   * Creates a new testkit instance from a user defined entity id and a function ValueEntityContext
   * to ValueEntity.
   */
  public static <S, E extends ValueEntity<S>> ValueEntityTestKit<S, E> of(
      String entityId, Function<ValueEntityContext, E> entityFactory) {
    TestKitValueEntityContext context = new TestKitValueEntityContext(entityId);
    return new ValueEntityTestKit<>(entityId, entityFactory.apply(context));
  }

  /** @return The current state of the value entity under test */
  public S getState() {
    return state;
  }

  private <Reply> ValueEntityResult<Reply> interpretEffects(ValueEntity.Effect<Reply> effect) {
    @SuppressWarnings("unchecked")
    ValueEntityResultImpl<Reply> result = new ValueEntityResultImpl<>(effect);
    if (result.stateWasUpdated()) {
      this.state = (S) result.getUpdatedState();
    } else if (result.stateWasDeleted()) {
      this.state = emptyState;
    }
    return result;
  }

  /**
   * The call method can be used to simulate a call to the ValueEntity. The passed java lambda
   * should return a ValueEntity.Effect. The Effect is interpreted into a ValueEntityResult that can
   * be used in test assertions.
   *
   * @param func A function from ValueEntity to ValueEntity.Effect.
   * @return a ValueEntityResult
   * @param <R> The type of reply that is expected from invoking a command handler
   */
  public <R> ValueEntityResult<R> call(Function<E, ValueEntity.Effect<R>> func) {
    return call(func, Metadata.EMPTY);
  }

  /**
   * The call method can be used to simulate a call to the ValueEntity. The passed java lambda
   * should return a ValueEntity.Effect. The Effect is interpreted into a ValueEntityResult that can
   * be used in test assertions.
   *
   * @param func     A function from ValueEntity to ValueEntity.Effect.
   * @param metadata A metadata passed as a call context.
   * @param <R>      The type of reply that is expected from invoking a command handler
   * @return a ValueEntityResult
   */
  public <R> ValueEntityResult<R> call(Function<E, ValueEntity.Effect<R>> func, Metadata metadata) {
    TestKitValueEntityCommandContext commandContext =
        new TestKitValueEntityCommandContext(entityId, metadata);
    entity._internalSetCommandContext(Optional.of(commandContext));
    entity._internalSetCurrentState(this.state);
    return interpretEffects(func.apply(entity));
  }
}
