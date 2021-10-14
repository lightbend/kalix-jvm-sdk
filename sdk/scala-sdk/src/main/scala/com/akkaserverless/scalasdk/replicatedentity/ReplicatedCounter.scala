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

package com.akkaserverless.scalasdk.replicatedentity

import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedCounterImpl
import com.akkaserverless.replicatedentity.ReplicatedData

/** A counter that can be incremented and decremented. */
class ReplicatedCounter private[scalasdk] (override val _internal: ReplicatedCounterImpl) extends ReplicatedData {

  /**
   * Get the current value of the counter.
   *
   * @return
   *   the current value of the counter
   */
  def value: Long = _internal.getValue

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
    new ReplicatedCounter(_internal.increment(amount))

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
    new ReplicatedCounter(_internal.decrement(amount))

}
