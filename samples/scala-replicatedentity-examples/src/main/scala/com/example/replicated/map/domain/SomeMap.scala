package com.example.replicated.map.domain

import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import kalix.scalasdk.replicatedentity.ReplicatedMap
import com.example.replicated.map
import com.google.protobuf.empty.Empty
import kalix.scalasdk.replicatedentity.ReplicatedRegister
import kalix.scalasdk.replicatedentity.ReplicatedSet

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class SomeMap(context: ReplicatedEntityContext) extends AbstractSomeMap {

  // keys
  private val FooKey = SomeKey("foo")
  private val BarKey = SomeKey("bar")
  private val BazKey = SomeKey("baz")

  // tag::update[]
  def increaseFoo(currentData: ReplicatedMap[SomeKey, ReplicatedData], increaseFooValue: map.IncreaseFooValue): ReplicatedEntity.Effect[Empty] = {
    val foo = currentData.getReplicatedCounter(FooKey) // <1>
    effects
      .update(currentData.update(FooKey, foo.increment(increaseFooValue.value)))// <2> <3>
      .thenReply(Empty.defaultInstance)
  }

  def decreaseFoo(currentData: ReplicatedMap[SomeKey, ReplicatedData], decreaseFooValue: map.DecreaseFooValue): ReplicatedEntity.Effect[Empty] = {
    val foo = currentData.getReplicatedCounter(FooKey) // <1>
    effects
      .update(currentData.update(FooKey, foo.decrement(decreaseFooValue.value))) // <2> <3>
      .thenReply(Empty.defaultInstance)
  }

  def setBar(currentData: ReplicatedMap[SomeKey, ReplicatedData], setBarValue: map.SetBarValue): ReplicatedEntity.Effect[Empty] = {
    val bar: ReplicatedRegister[String] = currentData.getReplicatedRegister(BarKey)
    effects
      .update(currentData.update(BarKey, bar.set(setBarValue.value)))
      .thenReply(Empty.defaultInstance)
  }

  def addBaz(currentData: ReplicatedMap[SomeKey, ReplicatedData], addBazValue: map.AddBazValue): ReplicatedEntity.Effect[Empty] = {
    val baz: ReplicatedSet[String] = currentData.getReplicatedSet(BazKey) // <1>
    effects
      .update(currentData.update(BazKey, baz.add(addBazValue.value))) // <2> <3>
      .thenReply(Empty.defaultInstance)
  }

  def removeBaz(currentData: ReplicatedMap[SomeKey, ReplicatedData], removeBazValue: map.RemoveBazValue): ReplicatedEntity.Effect[Empty] = {
    val baz: ReplicatedSet[String] = currentData.getReplicatedSet(BazKey) // <1>
    effects
      .update(currentData.update(BazKey, baz.remove(removeBazValue.value))) // <2> <3>
      .thenReply(Empty.defaultInstance)
  }
  // end::update[]

  // tag::get[]
  def get(currentData: ReplicatedMap[SomeKey, ReplicatedData], getValues: map.GetValues): ReplicatedEntity.Effect[map.CurrentValues] = {

    val foo = currentData.getReplicatedCounter(FooKey) // <1>
    val bar: ReplicatedRegister[String] = currentData.getReplicatedRegister(BarKey, () => "") // <1>
    val baz: ReplicatedSet[String] = currentData.getReplicatedSet(BazKey) // <1>

    val resp = map.CurrentValues(foo.value, bar(), baz.elements.toSeq)
    effects.reply(resp)
  }
  // end::get[]
}
