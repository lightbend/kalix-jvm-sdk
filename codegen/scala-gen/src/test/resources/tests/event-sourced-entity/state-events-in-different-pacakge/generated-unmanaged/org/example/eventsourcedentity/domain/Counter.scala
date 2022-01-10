package org.example.eventsourcedentity.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.google.protobuf.empty.Empty
import org.example.eventsourcedentity.counter_api
import org.example.eventsourcedentity.events.counter_events.Decreased
import org.example.eventsourcedentity.events.counter_events.Increased
import org.example.eventsourcedentity.state.counter_state.CounterState

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An event sourced entity. */
class Counter(context: EventSourcedEntityContext) extends AbstractCounter {
  override def emptyState: CounterState =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")

  override def increase(currentState: CounterState, increaseValue: counter_api.IncreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.error("The command handler for `Increase` is not implemented, yet")

  override def decrease(currentState: CounterState, decreaseValue: counter_api.DecreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.error("The command handler for `Decrease` is not implemented, yet")

  override def increased(currentState: CounterState, increased: Increased): CounterState =
    throw new RuntimeException("The event handler for `Increased` is not implemented, yet")

  override def decreased(currentState: CounterState, decreased: Decreased): CounterState =
    throw new RuntimeException("The event handler for `Decreased` is not implemented, yet")

}
