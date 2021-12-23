package org.example.eventsourcedentity;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An event sourced entity. */
public abstract class AbstractCounter extends EventSourcedEntity<CounterDomain.CounterState> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  /** Command handler for "Increase". */
  public abstract Effect<Empty> increase(CounterDomain.CounterState currentState, CounterApi.IncreaseValue increaseValue);

  /** Command handler for "Decrease". */
  public abstract Effect<Empty> decrease(CounterDomain.CounterState currentState, CounterApi.DecreaseValue decreaseValue);

  /** Event handler for "Increased". */
  public abstract CounterDomain.CounterState increased(CounterDomain.CounterState currentState, CounterDomain.Increased increased);

  /** Event handler for "Decreased". */
  public abstract CounterDomain.CounterState decreased(CounterDomain.CounterState currentState, CounterDomain.Decreased decreased);

}