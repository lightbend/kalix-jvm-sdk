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

  override def emptyValue: SomeValue =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty register value")


  /** Command handler for "Set". */
  def set(currentData: ReplicatedRegister[SomeValue], setValue: register.SetValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `Set` is not implemented, yet")

  /** Command handler for "Get". */
  def get(currentData: ReplicatedRegister[SomeValue], getValue: register.GetValue): ReplicatedEntity.Effect[register.CurrentValue] =
    effects.error("The command handler for `Get` is not implemented, yet")

}
