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

package com.example.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.testkit.EventSourcedResult;
import com.akkaserverless.javasdk.testkit.ActionResult;
import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
import com.example.actions.CounterJournalToTopicAction;
import com.akkaserverless.javasdk.testkit.impl.StubActionCreationContext;
import com.akkaserverless.javasdk.testkit.impl.StubActionContext;
import com.example.actions.CounterTopicApi;
import java.util.Optional;

/**
 * TestKit for unit testing Counter
 */
public final class CounterJournalToTopicActionTestKit {

  private Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory;

  private CounterJournalToTopicAction createAction() {
    CounterJournalToTopicAction action = actionFactory.apply(new StubActionCreationContext());
    action._internalSetActionContext(Optional.of(new StubActionContext()));
    return action;
  };
  
  /**
   * Create a testkit instance of Counter with a specific action id.
   */
  public static CounterJournalToTopicActionTestKit of(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
    return new CounterJournalToTopicActionTestKit(actionFactory);
  }

  /** Construction is done through the static CounterTestKit.of-methods */
  private CounterJournalToTopicActionTestKit(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
    this.actionFactory = actionFactory;
  }

  private <E> ActionResult<E> interpretEffects(Action.Effect<E> effect) {
    return new ActionResultImpl(effect);
  }

  public ActionResult<CounterTopicApi.Increased> increase(CounterDomain.ValueIncreased event) {
    Action.Effect<CounterTopicApi.Increased> effect = createAction().increase(event);
    return interpretEffects(effect);
  }

  public Action.Effect<CounterTopicApi.Decreased> decrease(CounterDomain.ValueDecreased event) {
    Action.Effect<CounterTopicApi.Decreased> effect = createAction().decrease(event);
    return effect;
  }


}