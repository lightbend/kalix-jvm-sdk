package com.example.replicated.counter.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.example.replicated.counter.SomeCounterApi;
import com.google.protobuf.Empty;

public class SomeCounter extends AbstractSomeCounter {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeCounter(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::update[]
  @Override
  public Effect<Empty> increase(ReplicatedCounter counter, SomeCounterApi.IncreaseValue command) {
    counter.increment(command.getValue()); // <1>
    return effects()
        .update(counter) // <2>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> decrease(ReplicatedCounter counter, SomeCounterApi.DecreaseValue command) {
    counter.decrement(command.getValue()); // <1>
    return effects()
        .update(counter) // <2>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeCounterApi.CurrentValue> get(
      ReplicatedCounter counter, SomeCounterApi.GetValue command) {
    long value = counter.getValue(); // <1>
    SomeCounterApi.CurrentValue currentValue =
        SomeCounterApi.CurrentValue.newBuilder().setValue(value).build();
    return effects().reply(currentValue);
  }
  // end::get[]
}
