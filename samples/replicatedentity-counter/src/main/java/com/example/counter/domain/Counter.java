package com.example.counter.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.example.counter.CounterApi;
import com.google.protobuf.Empty;

public class Counter extends AbstractCounter {
  @SuppressWarnings("unused")
  private final String entityId;

  public Counter(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Effect<Empty> increase(ReplicatedCounter counter, CounterApi.IncreaseValue command) {
    if (command.getValue() < 0) {
      return effects().error("Increase requires a positive value. It was [" + command.getValue() + "].");
    }

    counter.increment(command.getValue());

    return effects().update(counter).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> decrease(ReplicatedCounter counter, CounterApi.DecreaseValue command) {
    if (command.getValue() < 0) {
      return effects().error("Decrease requires a positive value. It was [" + command.getValue() + "].");
    }

    counter.decrement(command.getValue());

    return effects().update(counter).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<CounterApi.CurrentCounter> getCurrentCounter(ReplicatedCounter counter, CounterApi.GetCounter command) {
    CounterApi.CurrentCounter current = CounterApi.CurrentCounter.newBuilder().setValue(counter.getValue()).build();
    return effects().reply(current);
  }
}
