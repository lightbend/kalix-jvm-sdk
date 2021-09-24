package com.example.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.testkit.EventSourcedResult;
import com.example.actions.CounterJournalToTopicAction;
import com.akkaserverless.javasdk.testkit.impl.StubActionCreationContext;
import com.example.actions.CounterTopicApi;

/**
 * TestKit for unit testing Counter
 */
public final class CounterJournalToTopicActionTestKit {

  private CounterJournalToTopicAction action;
  private List<Object> events = new ArrayList<Object>();
  //I NEED A ACTIONCREATIONCONTEXT


  /**
   * Create a testkit instance of Counter with a specific action id.
   */
  public static CounterJournalToTopicActionTestKit of(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
    return new CounterJournalToTopicActionTestKit(actionFactory);
  }

  /** Construction is done through the static CounterTestKit.of-methods */
  private CounterJournalToTopicActionTestKit(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
    this.action = actionFactory(new StubActionCreationContext());
  }

  /**
   * @return All events that has been emitted by command handlers since the creation of this testkit.
   *         Individual sets of events from a single command handler invokation can be found in the
   *         Result from calling it.
   */
  public List<Object> getAllEvents() {
    return this.events;
  }

  // how to obtains a EventSourcedResult
    // create a new EventSourcedResultImpl? with no state as an input
        //but its implementation would be very much as secondaryEffects from EventSourcedResultImpl
            //shall I modify EventSourcedResultImpl?
  // private <Reply> EventSourcedResult<Reply> interpretEffects(EventSourcedEntity.Effect<Reply> effect) {
  //   List<Object> events = EventSourcedResultImpl.eventsOf(effect);
  //   this.events.addAll(events);
  //   return new EventSourcedResultImpl(effect, state);
  // }

  public EventSourcedResult<CounterTopicApi.Increased> increase(CounterDomain.ValueIncreased event) {
    Action.Effect<CounterTopicApi.Increased> effect = action.increase(event);
    return null;
  }

  // public EventSourcedResult<CounterTopicApi.Decreased> decrease(CounterDomain.ValueDecreased event) {
  //   EventSourcedEntity.Effect<CounterTopicApi.Decreased> effect = entity.decrease(event);
  //   return interpretEffects(effect);
  // }


}