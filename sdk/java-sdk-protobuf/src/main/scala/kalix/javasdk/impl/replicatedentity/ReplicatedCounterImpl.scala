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

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.replicatedentity.ReplicatedCounter
import kalix.protocol.replicated_entity.ReplicatedCounterDelta
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.replicatedentity.ReplicatedData

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

  override def toString = s"ReplicatedCounter($value)"

}
