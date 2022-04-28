package org.example.valueentity;

import com.google.protobuf.Empty;
import kalix.javasdk.valueentity.ValueEntity;
import org.example.Components;
import org.example.ComponentsImpl;
import org.example.valueentity.domain.CounterDomain;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractCounterServiceEntity extends ValueEntity<CounterDomain.CounterState> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> increase(CounterDomain.CounterState currentState, CounterApi.IncreaseValue increaseValue);

  public abstract Effect<Empty> decrease(CounterDomain.CounterState currentState, CounterApi.DecreaseValue decreaseValue);

}
