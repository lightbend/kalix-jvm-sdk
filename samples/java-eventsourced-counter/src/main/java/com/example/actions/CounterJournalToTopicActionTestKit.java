/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */

package com.example.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.Optional;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.testkit.ActionResult;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
import com.example.actions.CounterJournalToTopicAction;
import com.akkaserverless.javasdk.testkit.impl.StubActionCreationContext;
import com.akkaserverless.javasdk.testkit.impl.StubActionContext;
import com.example.actions.CounterTopicApi;
import com.example.domain.CounterDomain;

public final class CounterJournalToTopicActionTestKit {

  private Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory;

  private CounterJournalToTopicAction createAction() {
    CounterJournalToTopicAction action = actionFactory.apply(new StubActionCreationContext());
    action._internalSetActionContext(Optional.of(new StubActionContext()));
    return action;
  };
  
  public static CounterJournalToTopicActionTestKit of(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
    return new CounterJournalToTopicActionTestKit(actionFactory);
  }

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

  public ActionResult<CounterTopicApi.Decreased> decrease(CounterDomain.ValueDecreased event) {
    Action.Effect<CounterTopicApi.Decreased> effect = createAction().decrease(event);
    return interpretEffects(effect);
  }
}