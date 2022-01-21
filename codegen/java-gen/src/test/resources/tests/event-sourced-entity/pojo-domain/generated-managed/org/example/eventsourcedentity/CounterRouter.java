package org.example.eventsourcedentity;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.impl.JsonSerializer;
import com.akkaserverless.javasdk.impl.Serializers;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import com.google.protobuf.Empty;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * An event sourced entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command and event handler methods in the <code>Counter</code> class.
 */
public class CounterRouter extends EventSourcedEntityRouter<CounterState, Counter> {

  public CounterRouter(Counter entity) {
    super(entity);
  }

  @Override
  public CounterState handleEvent(CounterState state, Object event) {
    if (event instanceof Increased) {
      return entity().increased(state, (Increased) event);
    } else if (event instanceof Decreased) {
      return entity().decreased(state, (Decreased) event);
    } else {
      throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, CounterState state, Object command, CommandContext context) {
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
