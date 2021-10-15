package com.example.replicated.register.domain

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister
import com.example.replicated.register
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A replicated entity. */
class SomeRegister(context: ReplicatedEntityContext) extends AbstractSomeRegister {
  
  override def emptyValue: SomeValue = SomeValue.defaultInstance

  /** Command handler for "Set". */
  def set(currentData: ReplicatedRegister[SomeValue], setValue: register.SetValue): ReplicatedEntity.Effect[Empty] = {
    val someValue = SomeValue(setValue.value)
    effects
    .update(currentData.set(someValue))
    .thenReply(Empty.defaultInstance)
  }

  /** Command handler for "Get". */
  def get(currentData: ReplicatedRegister[SomeValue], getValue: register.GetValue): ReplicatedEntity.Effect[register.CurrentValue] = 
    effects.reply(register.CurrentValue(currentData().someField))
    
}
