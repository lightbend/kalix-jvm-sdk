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
  def set(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], setValue: registermap.SetValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `Set` is not implemented, yet")

  /** Command handler for "Remove". */
  def remove(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], removeValue: registermap.RemoveValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `Remove` is not implemented, yet")

  /** Command handler for "Get". */
  def get(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], getValue: registermap.GetValue): ReplicatedEntity.Effect[registermap.CurrentValue] =
    effects.error("The command handler for `Get` is not implemented, yet")

  /** Command handler for "GetAll". */
  def getAll(currentData: ReplicatedRegisterMap[SomeKey, SomeValue], getAllValues: registermap.GetAllValues): ReplicatedEntity.Effect[registermap.CurrentValues] =
    effects.error("The command handler for `GetAll` is not implemented, yet")

}
