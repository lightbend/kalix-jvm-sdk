package org.example.valueentity;

import com.akkaserverless.javasdk.impl.JsonSerializer;
import com.akkaserverless.javasdk.impl.Serializers;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractCounter extends ValueEntity<CounterState> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> increase(CounterState currentState, CounterApi.IncreaseValue increaseValue);

  public abstract Effect<Empty> decrease(CounterState currentState, CounterApi.DecreaseValue decreaseValue);

}
