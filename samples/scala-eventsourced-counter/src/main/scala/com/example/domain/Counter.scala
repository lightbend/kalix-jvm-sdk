
package com.example.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.example
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An event sourced entity. */
class Counter(context: EventSourcedEntityContext) extends AbstractCounter {
  override def emptyState: CounterState =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")

  override def increase(currentState: CounterState, increaseValue: example.IncreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.error("The command handler for `Increase` is not implemented, yet")

  override def decrease(currentState: CounterState, decreaseValue: example.DecreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.error("The command handler for `Decrease` is not implemented, yet")

  override def reset(currentState: CounterState, resetValue: example.ResetValue): EventSourcedEntity.Effect[Empty] =
    effects.error("The command handler for `Reset` is not implemented, yet")

  override def getCurrentCounter(currentState: CounterState, getCounter: example.GetCounter): EventSourcedEntity.Effect[example.CurrentCounter] =
    effects.error("The command handler for `GetCurrentCounter` is not implemented, yet")

  override def valueIncreased(currentState: CounterState, valueIncreased: ValueIncreased): CounterState =
    throw new RuntimeException("The event handler for `ValueIncreased` is not implemented, yet")

  override def valueDecreased(currentState: CounterState, valueDecreased: ValueDecreased): CounterState =
    throw new RuntimeException("The event handler for `ValueDecreased` is not implemented, yet")

  override def valueReset(currentState: CounterState, valueReset: ValueReset): CounterState =
    throw new RuntimeException("The event handler for `ValueReset` is not implemented, yet")
}
