/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import scala.collection.immutable.Set

import kalix.javasdk.impl.replicatedentity.ReplicatedCounterMapImpl
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

/**
 * A Map of counters. Uses [[kalix.scalasdk.replicatedentity.ReplicatedCounter]] 's as values.
 *
 * @tparam K
 *   The type for keys.
 */
class ReplicatedCounterMap[K] private[scalasdk] (override val delegate: ReplicatedCounterMapImpl[K])
    extends InternalReplicatedData {

  /**
   * Optionally returns the value associated with a key.
   *
   * @param key
   *   the key value
   * @return
   *   an option value containing the value associated with `key` in this map, or `None` if none exists.
   */

  def get(key: K): Option[Long] = delegate.getOption(key)

  /**
   * Get the counter value for the given key.
   *
   * @param key
   *   the key to get the value for
   * @return
   *   the current value of the counter at that key, or zero if no counter exists
   */
  def apply(key: K): Long = delegate.get(key)

  /**
   * Increment the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key
   *   the key of the counter
   * @param amount
   *   the amount to increment by
   * @return
   *   a new counter map with the incremented value
   */
  def increment(key: K, amount: Long): ReplicatedCounterMap[K] =
    new ReplicatedCounterMap(delegate.increment(key, amount))

  /**
   * Decrement the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key
   *   the key of the counter
   * @param amount
   *   the amount to decrement by
   * @return
   *   a new counter map with the decremented value
   */
  def decrement(key: K, amount: Long): ReplicatedCounterMap[K] =
    new ReplicatedCounterMap(delegate.decrement(key, amount))

  /**
   * Remove the mapping for a key if it is present.
   *
   * @param key
   *   key whose mapping is to be removed from the map
   * @return
   *   a new counter map with the removed mapping
   */
  def remove(key: K): ReplicatedCounterMap[K] =
    new ReplicatedCounterMap(delegate.remove(key))

  /**
   * Remove all mappings from this counter map.
   *
   * @return
   *   a new empty counter map
   */
  def clear(): ReplicatedCounterMap[K] =
    new ReplicatedCounterMap(delegate.clear())

  /**
   * Get the number of key-counter mappings in this counter map.
   *
   * @return
   *   the number of key-counter mappings in this counter map
   */
  def size: Int = delegate.size

  /**
   * Check whether this counter map is empty.
   *
   * @return
   *   `true` if this counter map contains no key-counter mappings
   */
  def isEmpty: Boolean = delegate.isEmpty

  /**
   * Check whether this counter map contains a mapping for the given key.
   *
   * @param key
   *   key whose presence in this map is to be tested
   * @return
   *   `true` if this counter map contains a mapping for the given key
   */
  def contains(key: K): Boolean = delegate.containsKey(key)

  /**
   * Tests whether a predicate holds for all elements of this ReplicatedCounterMap.
   *
   * @param predicate
   *   the predicate used to test elements.
   * @return
   *   `true` if this ReplicatedCounterMap is empty or the given predicate `pred` holds for all elements of this
   *   ReplicatedCounterMap, otherwise `false`.
   */
  def forall(predicate: ((K, Long)) => Boolean): Boolean =
    delegate.forall(predicate)

  /**
   * Get a [[scala.collection.immutable.Set]] view of the keys contained in this counter map.
   *
   * @return
   *   the keys contained in this counter map
   */
  def keySet: Set[K] = delegate.keys

  final override type Self = ReplicatedCounterMap[K]

  final override def resetDelta(): ReplicatedCounterMap[K] =
    new ReplicatedCounterMap(delegate.resetDelta())

  final override def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedCounterMap[K]] =
    delegate.applyDelta.andThen(new ReplicatedCounterMap(_))

  final override def toString: String = delegate.toString
}
