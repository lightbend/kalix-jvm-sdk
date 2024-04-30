/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
