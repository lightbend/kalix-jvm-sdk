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

  private CounterJournalToTopicAction action;
  private List<Object> effects = new ArrayList<Object>();
  
  /**
   * Create a testkit instance of Counter with a specific action id.
   */
  public static CounterJournalToTopicActionTestKit of(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
    return new CounterJournalToTopicActionTestKit(actionFactory);
  }

  /** Construction is done through the static CounterTestKit.of-methods */
  private CounterJournalToTopicActionTestKit(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
    this.action = actionFactory.apply(new StubActionCreationContext());
    this.action._internalSetActionContext(Optional.of(new StubActionContext()));
  }

  /**
   * @return All events that has been emitted by command handlers since the creation of this testkit.
   *         Individual sets of events from a single command handler invokation can be found in the
   *         Result from calling it.
   */
  public List<Object> getAllEvents() {
    return this.effects;
  }

  // how to obtains a EventSourcedResult
    // create a new EventSourcedResultImpl? with no state as an input
        //but its implementation would be very much as secondaryEffects from EventSourcedResultImpl
            //shall I modify EventSourcedResultImpl?
  private <E> ActionResult<E> interpretEffects(Action.Effect<E> effect) {
    this.effects.add(effect);
    return new ActionResultImpl(effect);
  }

  public ActionResult<CounterTopicApi.Increased> increase(CounterDomain.ValueIncreased event) {
    Action.Effect<CounterTopicApi.Increased> effect = action.increase(event);
    this.effects.add(effect);
    return interpretEffects(effect);
  }

  //look at ACtionEffectImpl

  public Action.Effect<CounterTopicApi.Decreased> decrease(CounterDomain.ValueDecreased event) {
    Action.Effect<CounterTopicApi.Decreased> effect = action.decrease(event);
    this.effects.add(effect);
    return effect;
  }


}