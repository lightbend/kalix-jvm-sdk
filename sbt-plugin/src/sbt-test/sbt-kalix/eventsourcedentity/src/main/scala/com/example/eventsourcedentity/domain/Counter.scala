package com.example.eventsourcedentity.domain

import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.example.eventsourcedentity
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class Counter(context: EventSourcedEntityContext) extends AbstractCounter {
  override def emptyState: CounterState =
    CounterState(0)

  override def increase(currentState: CounterState, increaseValue: eventsourcedentity.IncreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.emitEvent(Increased(increaseValue.value)).thenReply(_ => Empty())

  override def decrease(currentState: CounterState, decreaseValue: eventsourcedentity.DecreaseValue): EventSourcedEntity.Effect[Empty] =
    effects.emitEvent(Decreased(decreaseValue.value)).thenReply(_ => Empty())

  override def reset(currentState: CounterState, resetValue: eventsourcedentity.ResetValue): EventSourcedEntity.Effect[Empty] =
    if (currentState.value == 0)
      effects.reply(Empty())
    else if (currentState.value > 0)
      effects.emitEvent(Decreased(currentState.value)).thenReply(_ => Empty())
    else
      effects.emitEvent(Increased(-currentState.value)).thenReply(_ => Empty())

  override def getCurrentCounter(currentState: CounterState, getCounter: eventsourcedentity.GetCounter): EventSourcedEntity.Effect[eventsourcedentity.CurrentCounter] =
    effects.reply(eventsourcedentity.CurrentCounter(currentState.value))

  override def increased(currentState: CounterState, increased: Increased): CounterState =
    currentState.copy(value = currentState.value + increased.value)

  override def decreased(currentState: CounterState, decreased: Decreased): CounterState =
    currentState.copy(value = currentState.value - decreased.value)
}
