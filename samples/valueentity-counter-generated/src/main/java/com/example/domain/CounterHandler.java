/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package com.example.domain;

import com.akkaserverless.javasdk.impl.valueentity.ValueEntityHandler;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.example.CounterApi;
import com.google.protobuf.Empty;

/**
 * A value entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command handler methods in the <code>Counter</code> class.
 */
public class CounterHandler extends ValueEntityHandler<CounterDomain.CounterState, Counter> {

  public CounterHandler(Counter entity) {
    super(entity);
  }

  @Override
  public ValueEntityBase.Effect<?> handleCommand(
      String commandName, CounterDomain.CounterState state, Object command, CommandContext context) {
    switch (commandName) {

      case "Increase":
        return entity().increase(state, (CounterApi.IncreaseValue) command);

      case "Decrease":
        return entity().decrease(state, (CounterApi.DecreaseValue) command);

      case "Reset":
        return entity().reset(state, (CounterApi.ResetValue) command);

      case "GetCurrentCounter":
        return entity().getCurrentCounter(state, (CounterApi.GetCounter) command);

      default:
        throw new ValueEntityHandler.CommandHandlerNotFound(commandName);
    }
  }
}