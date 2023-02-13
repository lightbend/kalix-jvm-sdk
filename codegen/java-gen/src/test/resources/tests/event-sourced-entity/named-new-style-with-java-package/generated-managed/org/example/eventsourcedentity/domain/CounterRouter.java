package org.example.eventsourcedentity.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.CommandContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import org.example.eventsourcedentity.CounterApi;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * An event sourced entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command and event handler methods in the <code>Counter</code> class.
 */
public class CounterRouter extends EventSourcedEntityRouter<CounterDomain.CounterState, Object, Counter> {

  public CounterRouter(Counter entity) {
    super(entity);
  }

  @Override
  public CounterDomain.CounterState handleEvent(CounterDomain.CounterState state, Object event) {
    if (event instanceof CounterDomain.Increased) {
      return entity().increased(state, (CounterDomain.Increased) event);
    } else if (event instanceof CounterDomain.Decreased) {
      return entity().decreased(state, (CounterDomain.Decreased) event);
    } else {
      throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, CounterDomain.CounterState state, Object command, CommandContext context) {
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
