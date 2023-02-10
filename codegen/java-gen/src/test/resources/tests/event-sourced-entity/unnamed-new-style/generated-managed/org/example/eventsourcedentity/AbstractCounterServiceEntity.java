package org.example.eventsourcedentity;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.example.Components;
import org.example.ComponentsImpl;
import org.example.eventsourcedentity.domain.CounterDomain;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractCounterServiceEntity extends EventSourcedEntity<CounterDomain.CounterState, Object> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> increase(CounterDomain.CounterState currentState, CounterApi.IncreaseValue increaseValue);

  public abstract Effect<Empty> decrease(CounterDomain.CounterState currentState, CounterApi.DecreaseValue decreaseValue);

  public abstract CounterDomain.CounterState increased(CounterDomain.CounterState currentState, CounterDomain.Increased increased);

  public abstract CounterDomain.CounterState decreased(CounterDomain.CounterState currentState, CounterDomain.Decreased decreased);

}