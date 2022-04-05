package com.example.replicated.set.domain

import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import kalix.scalasdk.replicatedentity.ReplicatedSet
import com.example.replicated.set
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class SomeSet(context: ReplicatedEntityContext) extends AbstractSomeSet {

  // tag::update[]
  def add(currentData: ReplicatedSet[String], addElement: set.AddElement): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.add(addElement.element)) // <1>
      .thenReply(Empty.defaultInstance)

  def remove(currentData: ReplicatedSet[String], removeElement: set.RemoveElement): ReplicatedEntity.Effect[Empty] =
    effects
      .update(currentData.remove(removeElement.element)) // <1>
      .thenReply(Empty.defaultInstance)
  // end::update[]

  // tag::get[]
  def get(currentData: ReplicatedSet[String], getElements: set.GetElements): ReplicatedEntity.Effect[set.CurrentElements] =
    effects.reply(set.CurrentElements(currentData.elements.toSeq)) // <1>
  // end::get[]
}
