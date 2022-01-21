package org.example.valueentity;

import com.akkaserverless.javasdk.impl.JsonSerializer;
import com.akkaserverless.javasdk.impl.Serializers;
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityRouter;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A value entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command handler methods in the <code>Counter</code> class.
 */
public class CounterRouter extends ValueEntityRouter<CounterState, Counter> {

  public CounterRouter(Counter entity) {
    super(entity);
  }

  @Override
  public ValueEntity.Effect<?> handleCommand(
      String commandName, CounterState state, Object command, CommandContext context) {
    switch (commandName) {

      case "Increase":
        return entity().increase(state, (CounterApi.IncreaseValue) command);

      case "Decrease":
        return entity().decrease(state, (CounterApi.DecreaseValue) command);

      default:
        throw new ValueEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
