package org.example.eventsourcedentity;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.impl.JsonSerializer;
import com.akkaserverless.javasdk.impl.Serializers;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractCounter extends EventSourcedEntity<CounterState> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> increase(CounterState currentState, CounterApi.IncreaseValue increaseValue);

  public abstract Effect<Empty> decrease(CounterState currentState, CounterApi.DecreaseValue decreaseValue);

  public abstract CounterState increased(CounterState currentState, Increased increased);

  public abstract CounterState decreased(CounterState currentState, Decreased decreased);

}