/*
 * Copyright 2021 Lightbend Inc.
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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.GCounter
import com.akkaserverless.protocol.replicated_entity.{GCounterDelta, ReplicatedEntityDelta}

private[replicatedentity] final class GCounterImpl extends InternalReplicatedData with GCounter {
  override final val name = "GCounter"
  private var value: Long = 0
  private var deltaValue: Long = 0

  override def getValue: Long = value

  override def increment(by: Long): Long = {
    if (by < 0) {
      throw new IllegalArgumentException("Cannot increment a GCounter by a negative amount.")
    }
    deltaValue += by
    value += by
    value
  }

  override def hasDelta: Boolean = deltaValue != 0

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Gcounter(GCounterDelta(deltaValue))

  override def resetDelta(): Unit = deltaValue = 0

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.Gcounter(GCounterDelta(increment, _)) =>
      value += increment
  }

  override def toString = s"GCounter($value)"
}
