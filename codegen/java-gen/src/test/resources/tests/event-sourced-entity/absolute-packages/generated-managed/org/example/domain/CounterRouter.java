package org.example.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.CommandContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import org.example.events.OuterCounterEvents;
import org.example.eventsourcedentity.CounterApi;
import org.example.state.OuterCounterState;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * An event sourced entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command and event handler methods in the <code>Counter</code> class.
 */
public class CounterRouter extends EventSourcedEntityRouter<OuterCounterState.CounterState, Object, Counter> {

  public CounterRouter(Counter entity) {
    super(entity);
  }

  @Override
  public OuterCounterState.CounterState handleEvent(OuterCounterState.CounterState state, Object event) {
    if (event instanceof OuterCounterEvents.Increased) {
      return entity().increased(state, (OuterCounterEvents.Increased) event);
    } else if (event instanceof OuterCounterEvents.Decreased) {
      return entity().decreased(state, (OuterCounterEvents.Decreased) event);
    } else {
      throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, OuterCounterState.CounterState state, Object command, CommandContext context) {
    switch (commandName) {

      case "Increase":
        return entity().increase(state, (CounterApi.IncreaseValue) command);

      case "Decrease":
        return entity().decrease(state, (CounterApi.DecreaseValue) command);

      default:
        throw new EventSourcedEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
