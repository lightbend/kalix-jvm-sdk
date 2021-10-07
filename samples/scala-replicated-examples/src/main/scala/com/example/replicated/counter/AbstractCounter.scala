package com.example.replicated.counter

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedDataFactory
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.google.protobuf.empty.Empty

abstract class AbstractCounter extends ReplicatedEntity[ReplicatedCounter] {

  override def emptyData(factory: ReplicatedDataFactory): ReplicatedCounter =
    factory.newCounter

  def increase(counter: ReplicatedCounter, increaseValue: IncreaseValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(counter.increment(increaseValue.value))
      .thenReply(Empty.defaultInstance)

  def decrease(counter: ReplicatedCounter, decreaseValue: DecreaseValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(counter.decrement(decreaseValue.value))
      .thenReply(Empty.defaultInstance)

  def get(counter: ReplicatedCounter, getValue: GetValue): ReplicatedEntity.Effect[CurrentValue] =
    effects.reply(CurrentValue(counter.value))

}
