package org.example.domain;

import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;
import org.example.state.OuterCounterState;
import org.example.valueentity.CounterApi;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractCounter extends ValueEntity<OuterCounterState.CounterState> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> increase(OuterCounterState.CounterState currentState, CounterApi.IncreaseValue increaseValue);

  public abstract Effect<Empty> decrease(OuterCounterState.CounterState currentState, CounterApi.DecreaseValue decreaseValue);

}
