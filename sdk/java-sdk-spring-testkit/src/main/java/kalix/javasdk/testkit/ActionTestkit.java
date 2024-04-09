/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit;

import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.testkit.impl.ActionResultImpl;
import kalix.javasdk.testkit.impl.TestKitActionContext;
// TODO: abstract away reactor dependency
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Action Testkit for use in unit tests for Actions.
 *
 * <p>To test an Action create a testkit instance by calling one of the available {@code
 * ActionTestkit.of} methods. The returned testkit can be used as many times as you want. It doesn't
 * preserve any state between invocations.
 *
 * <p>Use the {@code call or stream} methods to interact with the testkit.
 */
public class ActionTestkit<A extends Action> {

  private final Function<ActionCreationContext, A> actionFactory;

  private ActionTestkit(Function<ActionCreationContext, A> actionFactory) {
    this.actionFactory = actionFactory;
  }

  public static <A extends Action> ActionTestkit<A> of(
      Function<ActionCreationContext, A> actionFactory) {
    return new ActionTestkit<>(actionFactory);
  }

  public static <A extends Action> ActionTestkit<A> of(Supplier<A> actionFactory) {
    return new ActionTestkit<>(ctx -> actionFactory.get());
  }

  private A createAction(TestKitActionContext context) {
    A action = actionFactory.apply(context);
    action._internalSetActionContext(Optional.of(context));
    return action;
  }

  /**
   * The {@code call} method can be used to simulate a unary call to the Action. The passed java lambda should
   * return an Action.Effect. The Effect is interpreted into an ActionResult that can be used in
   * test assertions.
   *
   * @param func A function from Action to Action.Effect
   * @return an ActionResult
   * @param <R> The type of reply that is expected from invoking a command handler
   */
  public <R> ActionResult<R> call(Function<A, Action.Effect<R>> func) {
    return call(func, Metadata.EMPTY);
  }

  /**
   * The {@code call} method can be used to simulate a unary call to the Action. The passed java lambda should
   * return an Action.Effect. The Effect is interpreted into an ActionResult that can be used in
   * test assertions.
   *
   * @param func     A function from Action to Action.Effect
   * @param metadata A metadata passed as a call context
   * @param <R>      The type of reply that is expected from invoking a command handler
   * @return an ActionResult
   */
  public <R> ActionResult<R> call(Function<A, Action.Effect<R>> func, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata, MockRegistry.EMPTY);
    return new ActionResultImpl<>(func.apply(createAction(context)));
  }

  /**
   * The {@code streamedCall} method can be used to simulate a streamed call to the Action. The passed java lambda should
   * return a {@code Flux<Action.Effect>}. The {@code Flux<Action.Effect>} is interpreted into an {@code Flux<ActionResult>} that can be used in
   * test assertions.
   *
   * @param func A function from {@code Flux<Action.Effect>} to a {@code Flux<ActionResult<R>>}
   * @return a {@code Flux<ActionResult<R>>}
   * @param <R> The type of reply that is expected from invoking a command handler
   */
  public <R> Flux<ActionResult<R>> streamedCall(Function<A, Flux<Action.Effect<R>>> func){
    TestKitActionContext var2 = new TestKitActionContext(Metadata.EMPTY, MockRegistry.EMPTY);
    Flux<Action.Effect<R>> res =  func.apply(this.createAction(var2));
    return res.map( i -> new ActionResultImpl<R>(i));
  }
}
