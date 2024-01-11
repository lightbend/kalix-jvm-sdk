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

import scala.collection.immutable.Set

import kalix.javasdk.impl.replicatedentity.ReplicatedMultiMapImpl
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

/**
 * A replicated map that maps keys to values, where each key may be associated with multiple values. Effectively a
 * ReplicatedMap with [[ReplicatedSet]] s as values.
 *
 * @tparam K
 *   The type for keys.
 * @tparam V
 *   The type for values.
 */
class ReplicatedMultiMap[K, V] private[scalasdk] (override val delegate: ReplicatedMultiMapImpl[K, V])
    extends InternalReplicatedData {

  /**
   * Get the values for the given key.
   *
   * @param key
   *   the key of the mapping
   * @return
   *   the current values at the given key, or an empty Set
   */
  def get(key: K): Set[V] =
    delegate.getValuesSet(key)

  /**
   * Store a key-value pair, if not already present.
   *
   * @param key
   *   the key of the mapping to add to
   * @param value
   *   the value to add to the mapping
   * @return
   *   a new multi-map with the additional value, or this unchanged multi-map
   */
  def put(key: K, value: V): ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(delegate.put(key, value))

  /**
   * Store multiple values for a key.
   *
   * @param key
   *   the key of the mapping to add to
   * @param values
   *   the values to add to the mapping
   * @return
   *   a new multi-map with the additional values, or this unchanged multi-map
   */
  def putAll(key: K, values: Iterable[V]): ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(delegate.putAll(key, values))

  /**
   * Remove a single key-value pair for the given key and value, if present.
   *
   * @param key
   *   the key of the mapping to remove from
   * @param value
   *   the value to remove from the mapping
   * @return
   *   a new multi-map with the removed value, or this unchanged multi-map
   */
  def remove(key: K, value: V): ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(delegate.remove(key, value))

  /**
   * Remove all values associated with the given key.
   *
   * @param key
   *   the key of the mapping to remove
   * @return
   *   a new multi-map with the removed mapping
   */
  def removeAll(key: K): ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(delegate.removeAll(key))

  /**
   * Remove all key-value pairs from the multi-map, leaving it empty.
   *
   * @return
   *   a new empty multi-map
   */
  def clear(): ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(delegate.clear())

  /**
   * Return the number of key-value pairs in this multi-map.
   *
   * <p>Note that this does not return the number of distinct keys, which is given by `keySet().size()`, but the total
   * number of values stored in the multi-map.
   *
   * @return
   *   the number of key-value pairs stored in this multi-map
   */
  def size: Int = delegate.size

  /**
   * Check whether this multi-map is empty.
   *
   * @return
   *   `true` if this multi-map contains no key-value pairs
   */
  def isEmpty: Boolean = delegate.isEmpty

  /**
   * Check whether this multi-map contains at least one value for the given key.
   *
   * @param key
   *   the key of the mapping to check
   * @return
   *   `true` if there is at least one key-value pair for the key
   */
  def contains(key: K): Boolean = delegate.containsKey(key)

  /**
   * Check whether this multi-map contains the given value associated with the given key.
   *
   * @param key
   *   the key of the mapping to check
   * @param value
   *   the value of the mapping to check
   * @return
   *   `true` if the key-value pair is in this multi-map
   */
  def containsValue(key: K, value: V): Boolean =
    delegate.containsValue(key, value)

  /**
   * Return the keys contained in this multi-map.
   *
   * <p>Note that the key set contains a key if and only if this multi-map maps that key to at least one value.
   *
   * @return
   *   the set of keys in this multi-map
   */
  def keySet: Set[K] = delegate.keys

  final override type Self = ReplicatedMultiMap[K, V]

  final override def resetDelta(): ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(delegate.resetDelta())

  final override def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedMultiMap[K, V]] =
    delegate.applyDelta.andThen(new ReplicatedMultiMap(_))
}
