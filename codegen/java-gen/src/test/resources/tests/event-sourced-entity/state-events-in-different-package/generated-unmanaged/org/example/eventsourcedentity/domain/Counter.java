package org.example.eventsourcedentity.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.example.eventsourcedentity.CounterApi;
import org.example.eventsourcedentity.events.OuterCounterEvents;
import org.example.eventsourcedentity.state.OuterCounterState;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Event Sourced Entity Service described in your counter_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class Counter extends AbstractCounter {

  @SuppressWarnings("unused")
  private final String entityId;

  public Counter(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public OuterCounterState.CounterState emptyState() {
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
  }

  @Override
  public Effect<Empty> increase(OuterCounterState.CounterState currentState, CounterApi.IncreaseValue increaseValue) {
    return effects().error("The command handler for `Increase` is not implemented, yet");
  }

  @Override
  public Effect<Empty> decrease(OuterCounterState.CounterState currentState, CounterApi.DecreaseValue decreaseValue) {
    return effects().error("The command handler for `Decrease` is not implemented, yet");
  }

  @Override
  public OuterCounterState.CounterState increased(OuterCounterState.CounterState currentState, OuterCounterEvents.Increased increased) {
    throw new RuntimeException("The event handler for `Increased` is not implemented, yet");
  }
  @Override
  public OuterCounterState.CounterState decreased(OuterCounterState.CounterState currentState, OuterCounterEvents.Decreased decreased) {
    throw new RuntimeException("The event handler for `Decreased` is not implemented, yet");
  }

}
