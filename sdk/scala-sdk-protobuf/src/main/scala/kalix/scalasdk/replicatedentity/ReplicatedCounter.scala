/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
