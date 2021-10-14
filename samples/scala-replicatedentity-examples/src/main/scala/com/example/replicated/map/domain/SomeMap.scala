package com.example.replicated.map.domain

import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap
import com.example.replicated.map
import com.google.protobuf.empty.Empty
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A replicated entity. */
class SomeMap(context: ReplicatedEntityContext) extends AbstractSomeMap {

  // keys
  private val FooKey = SomeKey("foo")
  private val BarKey = SomeKey("bar")
  private val BazKey = SomeKey("baz")

  /** Command handler for "IncreaseFoo". */
  def increaseFoo(currentData: ReplicatedMap[SomeKey, ReplicatedData], increaseFooValue: map.IncreaseFooValue): ReplicatedEntity.Effect[Empty] = {
    val foo = currentData.getReplicatedCounter(FooKey)
    effects
      .update(currentData.update(FooKey, foo.increment(increaseFooValue.value)))
      .thenReply(Empty.defaultInstance)
  }

  /** Command handler for "DecreaseFoo". */
  def decreaseFoo(currentData: ReplicatedMap[SomeKey, ReplicatedData], decreaseFooValue: map.DecreaseFooValue): ReplicatedEntity.Effect[Empty] = {
    val foo = currentData.getReplicatedCounter(FooKey)
    effects
      .update(currentData.update(FooKey, foo.decrement(decreaseFooValue.value)))
      .thenReply(Empty.defaultInstance)
  }

  /** Command handler for "SetBar". */
  def setBar(currentData: ReplicatedMap[SomeKey, ReplicatedData], setBarValue: map.SetBarValue): ReplicatedEntity.Effect[Empty] = {
    val bar: ReplicatedRegister[String] = currentData.getReplicatedRegister(BarKey)
    effects
      .update(currentData.update(BarKey, bar.set(setBarValue.value)))
      .thenReply(Empty.defaultInstance)
  }

  /** Command handler for "AddBaz". */
  def addBaz(currentData: ReplicatedMap[SomeKey, ReplicatedData], addBazValue: map.AddBazValue): ReplicatedEntity.Effect[Empty] = {
    val baz: ReplicatedSet[String] = currentData.getReplicatedSet(BazKey)
    effects
      .update(currentData.update(BarKey, baz.add(addBazValue.value)))
      .thenReply(Empty.defaultInstance)
  }

  /** Command handler for "RemoveBaz". */
  def removeBaz(currentData: ReplicatedMap[SomeKey, ReplicatedData], removeBazValue: map.RemoveBazValue): ReplicatedEntity.Effect[Empty] = {
val baz: ReplicatedSet[String] = currentData.getReplicatedSet(BazKey)
    effects
      .update(currentData.update(BarKey, baz.remove(removeBazValue.value)))
      .thenReply(Empty.defaultInstance)
  }

  /** Command handler for "Get". */
  def get(currentData: ReplicatedMap[SomeKey, ReplicatedData], getValues: map.GetValues): ReplicatedEntity.Effect[map.CurrentValues] = {

    val foo = currentData.getReplicatedCounter(FooKey)
    val bar: ReplicatedRegister[String] = currentData.getReplicatedRegister(BarKey, () => "")
    val baz: ReplicatedSet[String] = currentData.getReplicatedSet(BazKey)

    val resp = map.CurrentValues(foo.value, bar(), baz.elements.toSeq)
    effects.reply(resp)
  }
    
}
