package com.example.domain

import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import kalix.scalasdk.SideEffect
import com.example
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class Counter(context: EventSourcedEntityContext) extends AbstractCounter {
  override def emptyState: CounterState = CounterState()

  override def increase(
      currentState: CounterState,
      increaseValue: example.IncreaseValue): EventSourcedEntity.Effect[Empty] =
    if (increaseValue.value < 0) {
      effects.error("Value must be a zero or a positive number")
    } else {
      effects
        .emitEvent(ValueIncreased(increaseValue.value))
        .thenReply(_ => Empty())
    }

  override def increaseWithConditional(
      currentState: CounterState,
      increaseValue: example.IncreaseValue): EventSourcedEntity.Effect[Empty] =
    if (increaseValue.value < 0) {
      effects.error("Value must be a zero or a positive number")
    } else {
      val valIncrease = if (commandContext.metadata.get("myKey") == Some("myValue")){
        ValueIncreased(2 * increaseValue.value) 
      } else {
        ValueIncreased(increaseValue.value)
      }
      effects
        .emitEvent(valIncrease)
        .thenReply(_ => Empty())
    }

  override def increaseWithSideEffect(
      currentState: CounterState,
      increaseValue: example.IncreaseValue): EventSourcedEntity.Effect[Empty] =
    if (increaseValue.value < 0) {
      effects.error("Value must be a zero or a positive number")
    } else {
      components //this checks we have access to the entity's context
      effects
        .emitEvent(ValueIncreased(increaseValue.value))
        .thenAddSideEffect(_ =>
          SideEffect(components.counter.increase(example.IncreaseValue(context.entityId, increaseValue.value * 2))))
        .thenReply(_ => Empty())
    }

  override def decrease(
      currentState: CounterState,
      decreaseValue: example.DecreaseValue): EventSourcedEntity.Effect[Empty] =
    if (decreaseValue.value < 0) {
      effects.error("Value must be a zero or a positive number")
    } else {
      effects
        .emitEvent(ValueDecreased(decreaseValue.value))
        .thenReply(_ => Empty())
    }

  override def reset(currentState: CounterState, resetValue: example.ResetValue): EventSourcedEntity.Effect[Empty] =
    effects
      .emitEvent(ValueReset())
      .thenReply(_ => Empty())

  override def getCurrentCounter(
      currentState: CounterState,
      getCounter: example.GetCounter): EventSourcedEntity.Effect[example.CurrentCounter] =
    effects.reply(example.CurrentCounter(currentState.value))

  override def valueIncreased(currentState: CounterState, valueIncreased: ValueIncreased): CounterState =
    currentState.copy(value = currentState.value + valueIncreased.value)

  override def valueDecreased(currentState: CounterState, valueDecreased: ValueDecreased): CounterState =
    currentState.copy(value = currentState.value - valueDecreased.value)

  override def valueReset(currentState: CounterState, valueReset: ValueReset): CounterState =
    emptyState
}
