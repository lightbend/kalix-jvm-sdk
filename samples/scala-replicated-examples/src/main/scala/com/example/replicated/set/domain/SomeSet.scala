package com.example.replicated.set.domain

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet
import com.example.replicated.set
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A replicated entity. */
class SomeSet(context: ReplicatedEntityContext) extends AbstractSomeSet {


  /** Command handler for "Add". */
  def add(currentData: ReplicatedSet[String], addElement: set.AddElement): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `Add` is not implemented, yet")

  /** Command handler for "Remove". */
  def remove(currentData: ReplicatedSet[String], removeElement: set.RemoveElement): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `Remove` is not implemented, yet")

  /** Command handler for "Get". */
  def get(currentData: ReplicatedSet[String], getElements: set.GetElements): ReplicatedEntity.Effect[set.CurrentElements] =
    effects.error("The command handler for `Get` is not implemented, yet")

}
