package com.example.replicated.registermap.domain

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap
import com.example.replicated.registermap
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A replicated entity. */
class SomeRegisterMap(context: ReplicatedEntityContext) extends AbstractSomeRegisterMap {

  /** Command handler for "Set". */
  def set(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], setValue: registermap.SetValue): ReplicatedEntity.Effect[Empty] = {
    
    val keyValue = 
      for {
        key <- setValue.key.map(_.field)
        value <- setValue.value.map(_.field)
      } yield (key, value)
    
      keyValue.map { case (key, value) => 
        effects
          .update(currentData.setValue(SomeKey(key), SomeValue(value)))
          .thenReply(Empty.defaultInstance)
      }
      .getOrElse {
        effects
          .error(s"Invalid input: received key = '${setValue.key}' and value = '${setValue.value}'")
      }
    
  }

  /** Command handler for "Remove". */
  def remove(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], removeValue: registermap.RemoveValue): ReplicatedEntity.Effect[Empty] = 
    removeValue.key.map { key => 
      effects
        .update(currentData.remove(SomeKey(key.field)))
        .thenReply(Empty.defaultInstance)
    }
    .getOrElse { 
      effects.error("Invalid input: key is empty")
    }
  

  /** Command handler for "Get". */
  def get(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], getValue: registermap.GetValue): ReplicatedEntity.Effect[registermap.CurrentValue] = 
    getValue.key.map { key => 
      val value = currentData.get(SomeKey(key.field)).map(v => registermap.Value(v.someField))
      effects
        .reply(registermap.CurrentValue(getValue.key, value))
    }
    .getOrElse {
      effects.error("The command handler for `Get` is not implemented, yet")
    }

  /** Command handler for "GetAll". */
  def getAll(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], getAllValues: registermap.GetAllValues): ReplicatedEntity.Effect[registermap.CurrentValues] = {

    val allData = 
      currentData.keySet.map { key => 
        val value = currentData.get(key).map(v => registermap.Value(v.someField))
        registermap.CurrentValue(Some(registermap.Key(key.someField)), value)
      }.toSeq

    effects.reply(registermap.CurrentValues(allData))
  }

}
