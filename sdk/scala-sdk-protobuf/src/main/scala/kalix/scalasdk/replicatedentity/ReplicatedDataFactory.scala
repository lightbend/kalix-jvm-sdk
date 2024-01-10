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

import kalix.replicatedentity.ReplicatedData

trait ReplicatedDataFactory {

  /** Create a new counter. */
  def newCounter: ReplicatedCounter

  /** Create a new map of counters. */
  def newReplicatedCounterMap[K]: ReplicatedCounterMap[K]

  /** Create a new ReplicatedSet. */
  def newReplicatedSet[E]: ReplicatedSet[E]

  /** Create a new multimap (map of sets). */
  def newReplicatedMultiMap[K, V]: ReplicatedMultiMap[K, V]

  /** Create a new ReplicatedRegister. */
  def newRegister[T](value: T): ReplicatedRegister[T]

  /** Create a new map of registers. */
  def newReplicatedRegisterMap[K, V]: ReplicatedRegisterMap[K, V]

  /** Create a new ReplicatedMap. */
  def newReplicatedMap[K, V <: ReplicatedData]: ReplicatedMap[K, V]

  /** Create a new Vote. */
  def newVote: ReplicatedVote
}
