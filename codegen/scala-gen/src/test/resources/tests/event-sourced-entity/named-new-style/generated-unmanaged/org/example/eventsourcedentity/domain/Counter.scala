package org.example.eventsourcedentity.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import org.example.eventsourcedentity

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class Counter(context: EventSourcedEntityContext) extends AbstractCounter {
  override def emptyState: CounterState =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")

  override def increase(currentState: CounterState, increaseValue: eventsourcedentity.IncreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.error("The command handler for `Increase` is not implemented, yet")

  override def decrease(currentState: CounterState, decreaseValue: eventsourcedentity.DecreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.error("The command handler for `Decrease` is not implemented, yet")

  override def increased(currentState: CounterState, increased: Increased): CounterState =
    throw new RuntimeException("The event handler for `Increased` is not implemented, yet")

  override def decreased(currentState: CounterState, decreased: Decreased): CounterState =
    throw new RuntimeException("The event handler for `Decreased` is not implemented, yet")

}
