// FIXME codegen for ReplicatedEntity

package com.example.counter.domain;

import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityHandler;
import com.akkaserverless.javasdk.replicatedentity.CommandContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
import com.example.counter.CounterApi;
import com.google.protobuf.Empty;

public class CounterHandler extends ReplicatedEntityHandler<ReplicatedCounter, Counter> {

  public CounterHandler(Counter entity) {
    super(entity);
  }

  @Override
  public ReplicatedEntity.Effect<?> handleCommand(
      String commandName, ReplicatedCounter data, Object command, CommandContext context) {
    switch (commandName) {

      case "Increase":
        return entity().increase(data, (CounterApi.IncreaseValue) command);

      case "Decrease":
        return entity().decrease(data, (CounterApi.DecreaseValue) command);

      case "GetCurrentCounter":
        return entity().getCurrentCounter(data, (CounterApi.GetCounter) command);

      default:
        throw new ReplicatedEntityHandler.CommandHandlerNotFound(commandName);
    }
  }
}
