package com.example.replicated.countermap.domain

import kalix.scalasdk.replicatedentity.ReplicatedCounterMap
import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import com.example.replicated.countermap
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class SomeCounterMap(context: ReplicatedEntityContext) extends AbstractSomeCounterMap {

  // tag::update[]
  def increase(currentData: ReplicatedCounterMap[String], increaseValue: countermap.IncreaseValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.increment(increaseValue.key, increaseValue.value)) // <1>
      .thenReply(Empty.defaultInstance)

  def decrease(currentData: ReplicatedCounterMap[String], decreaseValue: countermap.DecreaseValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.decrement(decreaseValue.key, decreaseValue.value)) // <1>
      .thenReply(Empty.defaultInstance)

  def remove(currentData: ReplicatedCounterMap[String], removeValue: countermap.RemoveValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.remove(removeValue.key)) // <1>
      .thenReply(Empty.defaultInstance)
  // end::update[]

  // tag::get[]
  def get(currentData: ReplicatedCounterMap[String], getValue: countermap.GetValue): ReplicatedEntity.Effect[countermap.CurrentValue] = {
    val value = currentData(getValue.key) // <1>
    effects.reply(countermap.CurrentValue(value))
  }

  def getAll(currentData: ReplicatedCounterMap[String], getAllValues: countermap.GetAllValues): ReplicatedEntity.Effect[countermap.CurrentValues] = {
    val keyValues = currentData.keySet.map { key => key -> currentData(key) }.toMap // <2>
    effects.reply(countermap.CurrentValues(keyValues))
  }
  // end::get[]
}
