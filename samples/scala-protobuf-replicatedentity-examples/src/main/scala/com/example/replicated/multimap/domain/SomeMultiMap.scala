package com.example.replicated.multimap.domain

import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import kalix.scalasdk.replicatedentity.ReplicatedMultiMap
import com.example.replicated.multimap
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class SomeMultiMap(context: ReplicatedEntityContext) extends AbstractSomeMultiMap {

  // tag::update[]
  def put(currentData: ReplicatedMultiMap[String, Double], putValue: multimap.PutValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.put(putValue.key, putValue.value)) // <1>
      .thenReply(Empty.defaultInstance)

  def putAll(currentData: ReplicatedMultiMap[String, Double], putAllValues: multimap.PutAllValues): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.putAll(putAllValues.key, putAllValues.values)) // <1>
      .thenReply(Empty.defaultInstance)

  def remove(currentData: ReplicatedMultiMap[String, Double], removeValue: multimap.RemoveValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.remove(removeValue.key, removeValue.value)) // <1>
      .thenReply(Empty.defaultInstance)

  def removeAll(currentData: ReplicatedMultiMap[String, Double], removeAllValues: multimap.RemoveAllValues): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.removeAll(removeAllValues.key)) // <1>
      .thenReply(Empty.defaultInstance)
  // end::update[]

  // tag::get[]
  def get(currentData: ReplicatedMultiMap[String, Double], getValues: multimap.GetValues): ReplicatedEntity.Effect[multimap.CurrentValues] = {
    val values = currentData.get(getValues.key) // <1>
    effects
      .reply(multimap.CurrentValues(getValues.key, values.toSeq))
  }

  /** Command handler for "GetAll". */
  def getAll(currentData: ReplicatedMultiMap[String, Double], getAllValues: multimap.GetAllValues): ReplicatedEntity.Effect[multimap.AllCurrentValues] = {
    val currentValues =
      currentData.keySet.map { key => // <2>
        val values = currentData.get(key)
        multimap.CurrentValues(key, values.toSeq)
      }

    effects.reply(multimap.AllCurrentValues(currentValues.toSeq))
  }
  // end::get[]

}
