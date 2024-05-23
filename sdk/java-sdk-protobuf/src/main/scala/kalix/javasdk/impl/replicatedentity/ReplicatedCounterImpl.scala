/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.replicatedentity.ReplicatedCounter
import kalix.protocol.replicated_entity.ReplicatedCounterDelta
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

private[kalix] final class ReplicatedCounterImpl(value: Long = 0, delta: Long = 0)
    extends ReplicatedCounter
    with InternalReplicatedData {

  override type Self = ReplicatedCounterImpl
  override val name = "ReplicatedCounter"

  override def getValue: Long = value

  override def increment(amount: Long): ReplicatedCounterImpl =
    new ReplicatedCounterImpl(value + amount, delta + amount)

  override def decrement(amount: Long): ReplicatedCounterImpl = increment(-amount)

  override def hasDelta: Boolean = delta != 0

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Counter(ReplicatedCounterDelta(delta))

  override def resetDelta(): ReplicatedCounterImpl =
    if (hasDelta) new ReplicatedCounterImpl(value) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedCounterImpl] = {
    case ReplicatedEntityDelta.Delta.Counter(ReplicatedCounterDelta(increment, _)) =>
      new ReplicatedCounterImpl(value + increment)
  }

  override def toString: String = s"ReplicatedCounter($value)"

}
