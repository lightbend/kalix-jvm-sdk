package com.example.replicated.map.domain

import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap
import com.example.replicated.map
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A replicated entity. */
class SomeMap(context: ReplicatedEntityContext) extends AbstractSomeMap {


  /** Command handler for "IncreaseFoo". */
  def increaseFoo(currentData: ReplicatedMap[SomeKey, ReplicatedData], increaseFooValue: map.IncreaseFooValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `IncreaseFoo` is not implemented, yet")

  /** Command handler for "DecreaseFoo". */
  def decreaseFoo(currentData: ReplicatedMap[SomeKey, ReplicatedData], decreaseFooValue: map.DecreaseFooValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `DecreaseFoo` is not implemented, yet")

  /** Command handler for "SetBar". */
  def setBar(currentData: ReplicatedMap[SomeKey, ReplicatedData], setBarValue: map.SetBarValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `SetBar` is not implemented, yet")

  /** Command handler for "AddBaz". */
  def addBaz(currentData: ReplicatedMap[SomeKey, ReplicatedData], addBazValue: map.AddBazValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `AddBaz` is not implemented, yet")

  /** Command handler for "RemoveBaz". */
  def removeBaz(currentData: ReplicatedMap[SomeKey, ReplicatedData], removeBazValue: map.RemoveBazValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `RemoveBaz` is not implemented, yet")

  /** Command handler for "Get". */
  def get(currentData: ReplicatedMap[SomeKey, ReplicatedData], getValues: map.GetValues): ReplicatedEntity.Effect[map.CurrentValues] =
    effects.error("The command handler for `Get` is not implemented, yet")

}
