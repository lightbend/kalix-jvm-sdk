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

  //look at ACtionEffectImpl

  public Action.Effect<CounterTopicApi.Decreased> decrease(CounterDomain.ValueDecreased event) {
    Action.Effect<CounterTopicApi.Decreased> effect = createAction().decrease(event);
    return effect;
  }


}