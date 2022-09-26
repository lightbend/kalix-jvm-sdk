/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.springsdk.testkit;

import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.MockRegistry;
import kalix.javasdk.testkit.impl.ActionResultImpl;
import kalix.javasdk.testkit.impl.TestKitActionContext;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Action Testkit for use in unit tests for Actions.
 *
 * <p>To test an Action create a testkit instance by calling one of the available {@code
 * ActionTestkit.of} methods. The returned testkit can be used as many times as you want. It doesn't
 * preserve any state between invocation.
 *
 * <p>Use the {@code call} methods to interact with the testkit.
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
   * The call method can be used to simulate a call to the Action. That passed java lambda should
   * return an Action.Effect. The Effect is interpreted into an ActionResult that can be used in
   * test assertions.
   *
   * @param func A function from Action to Action.Effect
   * @return an ActionResult
   * @param <R> The type of reply that is expected from invoking a command handler
   */
  public <R> ActionResult<R> call(Function<A, Action.Effect<R>> func) {
    TestKitActionContext context = new TestKitActionContext(Metadata.EMPTY, MockRegistry.EMPTY);
    return new ActionResultImpl<>(func.apply(createAction(context)));
  }
}
