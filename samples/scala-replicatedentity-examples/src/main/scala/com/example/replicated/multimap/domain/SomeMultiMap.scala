package com.example.replicated.multimap.domain

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap
import com.example.replicated.multimap
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A replicated entity. */
class SomeMultiMap(context: ReplicatedEntityContext) extends AbstractSomeMultiMap {

  /** Command handler for "Put". */
  def put(currentData: ReplicatedMultiMap[String, Double], putValue: multimap.PutValue): ReplicatedEntity.Effect[Empty] = 
    effects
      .update(currentData.put(putValue.key, putValue.value))
      .thenReply(Empty.defaultInstance)

  /** Command handler for "PutAll". */
  def putAll(currentData: ReplicatedMultiMap[String, Double], putAllValues: multimap.PutAllValues): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.putAll(putAllValues.key, putAllValues.values))
      .thenReply(Empty.defaultInstance)

  /** Command handler for "Remove". */
  def remove(currentData: ReplicatedMultiMap[String, Double], removeValue: multimap.RemoveValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.remove(removeValue.key, removeValue.value))
      .thenReply(Empty.defaultInstance)

  /** Command handler for "RemoveAll". */
  def removeAll(currentData: ReplicatedMultiMap[String, Double], removeAllValues: multimap.RemoveAllValues): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.removeAll(removeAllValues.key))
      .thenReply(Empty.defaultInstance)

  /** Command handler for "Get". */
  def get(currentData: ReplicatedMultiMap[String, Double], getValues: multimap.GetValues): ReplicatedEntity.Effect[multimap.CurrentValues] = {
    val values = currentData.get(getValues.key)
    effects
      .reply(multimap.CurrentValues(getValues.key, values.toSeq))
  }
    
  /** Command handler for "GetAll". */
  def getAll(currentData: ReplicatedMultiMap[String, Double], getAllValues: multimap.GetAllValues): ReplicatedEntity.Effect[multimap.AllCurrentValues] = {
    val currentValues = 
      currentData.keySet.map { key => 
        val values = currentData.get(key)
        multimap.CurrentValues(key, values.toSeq)
      }

    effects.reply(multimap.AllCurrentValues(currentValues.toSeq))
  }
    
}
