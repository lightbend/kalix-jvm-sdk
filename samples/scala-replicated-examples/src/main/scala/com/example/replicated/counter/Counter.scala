package com.example.replicated.counter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.google.protobuf.empty.Empty

class Counter(context: ReplicatedEntityContext) extends AbstractCounter {

  override def increase(counter: ReplicatedCounter, increaseValue: IncreaseValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(counter.increment(increaseValue.value))
      .thenReply(Empty.defaultInstance)

  override def decrease(counter: ReplicatedCounter, decreaseValue: DecreaseValue): ReplicatedEntity.Effect[Empty] =
    effects
      .update(counter.decrement(decreaseValue.value))
      .thenReply(Empty.defaultInstance)

  override def get(counter: ReplicatedCounter, getValue: GetValue): ReplicatedEntity.Effect[CurrentValue] =
    effects.reply(CurrentValue(counter.value))

}
