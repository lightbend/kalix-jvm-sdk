/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import kalix.javasdk.impl.replicatedentity.ReplicatedCounterImpl
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

/** A counter that can be incremented and decremented. */
class ReplicatedCounter private[scalasdk] (override val delegate: ReplicatedCounterImpl)
    extends InternalReplicatedData {

  /**
   * Get the current value of the counter.
   *
   * @return
   *   the current value of the counter
   */
  def value: Long = delegate.getValue

  /**
   * Increment the counter.
   *
   * If `amount` is negative, then the counter will be decremented by that much instead.
   *
   * @param amount
   *   the amount to increment the counter by
   * @return
   *   a new counter with incremented value
   */
  def increment(amount: Long): ReplicatedCounter =
    new ReplicatedCounter(delegate.increment(amount))

  /**
   * Decrement the counter.
   *
   * If `amount` is negative, then the counter will be incremented by that much instead.
   *
   * @param amount
   *   the amount to decrement the counter by
   * @return
   *   a new counter with decremented value
   */
  def decrement(amount: Long): ReplicatedCounter =
    new ReplicatedCounter(delegate.decrement(amount))

  final override type Self = ReplicatedCounter

  final override def resetDelta(): ReplicatedCounter =
    new ReplicatedCounter(delegate.resetDelta())

  final override def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedCounter] =
    delegate.applyDelta.andThen(new ReplicatedCounter(_))
}
