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

package kalix.javasdk.replicatedentity;

import kalix.replicatedentity.ReplicatedData;

/** A counter that can be incremented and decremented. */
public interface ReplicatedCounter extends ReplicatedData {
  /**
   * Get the current value of the counter.
   *
   * @return the current value of the counter
   */
  long getValue();

  /**
   * Increment the counter.
   *
   * <p>If <code>amount</code> is negative, then the counter will be decremented by that much
   * instead.
   *
   * @param amount the amount to increment the counter by
   * @return a new counter with incremented value
   */
  ReplicatedCounter increment(long amount);

  /**
   * Decrement the counter.
   *
   * <p>If <code>amount</code> is negative, then the counter will be incremented by that much
   * instead.
   *
   * @param amount the amount to decrement the counter by
   * @return a new counter with decremented value
   */
  ReplicatedCounter decrement(long amount);
}
