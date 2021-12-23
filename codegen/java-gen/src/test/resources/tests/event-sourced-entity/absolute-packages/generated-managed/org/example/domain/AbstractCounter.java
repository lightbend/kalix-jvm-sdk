package org.example.domain;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;
import org.example.events.OuterCounterEvents;
import org.example.eventsourcedentity.CounterApi;
import org.example.state.OuterCounterState;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An event sourced entity. */
public abstract class AbstractCounter extends EventSourcedEntity<OuterCounterState.CounterState> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  /** Command handler for "Increase". */
  public abstract Effect<Empty> increase(OuterCounterState.CounterState currentState, CounterApi.IncreaseValue increaseValue);

  /** Command handler for "Decrease". */
  public abstract Effect<Empty> decrease(OuterCounterState.CounterState currentState, CounterApi.DecreaseValue decreaseValue);

  /** Event handler for "Increased". */
  public abstract OuterCounterState.CounterState increased(OuterCounterState.CounterState currentState, OuterCounterEvents.Increased increased);

  /** Event handler for "Decreased". */
  public abstract OuterCounterState.CounterState decreased(OuterCounterState.CounterState currentState, OuterCounterEvents.Decreased decreased);

}