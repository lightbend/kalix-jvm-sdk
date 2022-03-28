package com.example.replicated.registermap.domain

import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import kalix.scalasdk.replicatedentity.ReplicatedRegisterMap
import com.example.replicated.registermap
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class SomeRegisterMap(context: ReplicatedEntityContext) extends AbstractSomeRegisterMap {

  // tag::update[]
  def set(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], setValue: registermap.SetValue): ReplicatedEntity.Effect[Empty] = {
    val key = SomeKey(setValue.getKey.field) // <1>
    val value = SomeValue(setValue.getValue.field) // <2>
    effects
      .update(currentData.setValue(key,value)) // <3>
      .thenReply(Empty.defaultInstance)
  }

  def remove(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], removeValue: registermap.RemoveValue): ReplicatedEntity.Effect[Empty] = {
    val key = SomeKey(removeValue.getKey.field) // <1>
    effects
      .update(currentData.remove(key)) // <3>
      .thenReply(Empty.defaultInstance)
  }
  // end::update[]

  // tag::get[]
  def get(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], getValue: registermap.GetValue): ReplicatedEntity.Effect[registermap.CurrentValue] = {
    val key = SomeKey(getValue.getKey.field) // <1>
    val maybeValue = currentData.get(key) // <2>
    val currentValue = registermap.CurrentValue(getValue.key, maybeValue.map(v => registermap.Value(v.someField)))
    effects.reply(currentValue)
  }

  def getAll(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], getAllValues: registermap.GetAllValues): ReplicatedEntity.Effect[registermap.CurrentValues] = {

    val allData =
      currentData.keySet.map { key => // <3>
        val value = currentData.get(key).map(v => registermap.Value(v.someField))
        registermap.CurrentValue(Some(registermap.Key(key.someField)), value)
      }.toSeq

    effects.reply(registermap.CurrentValues(allData))
  }
  // end::get[]

}
