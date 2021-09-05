// FIXME codegen for ReplicatedEntity

package com.example.counter.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
import com.example.counter.CounterApi;
import com.google.protobuf.Empty;

public abstract class AbstractCounter extends ReplicatedEntity<ReplicatedCounter> {

  public abstract Effect<Empty> increase(ReplicatedCounter currentData, CounterApi.IncreaseValue command);

  public abstract Effect<Empty> decrease(ReplicatedCounter currentData, CounterApi.DecreaseValue command);

  public abstract Effect<CounterApi.CurrentCounter> getCurrentCounter(ReplicatedCounter currentData, CounterApi.GetCounter command);

}
