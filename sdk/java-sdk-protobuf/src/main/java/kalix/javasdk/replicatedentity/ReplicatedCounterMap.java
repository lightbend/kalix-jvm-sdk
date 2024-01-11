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
import java.util.Set;

/**
 * A Map of counters. Uses {@link kalix.javasdk.replicatedentity.ReplicatedCounter}'s as values.
 *
 * @param <K> The type for keys.
 */
public interface ReplicatedCounterMap<K> extends ReplicatedData {

  /**
   * Get the counter value for the given key.
   *
   * @param key the key to get the value for
   * @return the current value of the counter at that key, or zero if no counter exists
   */
  long get(K key);

  /**
   * Increment the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key the key of the counter
   * @param amount the amount to increment by
   * @return a new counter map with the incremented value
   */
  ReplicatedCounterMap<K> increment(K key, long amount);

  /**
   * Decrement the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key the key of the counter
   * @param amount the amount to decrement by
   * @return a new counter map with the decremented value
   */
  ReplicatedCounterMap<K> decrement(K key, long amount);

  /**
   * Remove the mapping for a key if it is present.
   *
   * @param key key whose mapping is to be removed from the map
   * @return a new counter map with the removed mapping
   */
  ReplicatedCounterMap<K> remove(K key);

  /**
   * Remove all mappings from this counter map.
   *
   * @return a new empty counter map
   */
  ReplicatedCounterMap<K> clear();

  /**
   * Get the number of key-counter mappings in this counter map.
   *
   * @return the number of key-counter mappings in this counter map
   */
  int size();

  /**
   * Check whether this counter map is empty.
   *
   * @return {@code true} if this counter map contains no key-counter mappings
   */
  boolean isEmpty();

  /**
   * Check whether this counter map contains a mapping for the given key.
   *
   * @param key key whose presence in this map is to be tested
   * @return {@code true} if this counter map contains a mapping for the given key
   */
  boolean containsKey(K key);

  /**
   * Get a {@link Set} view of the keys contained in this counter map.
   *
   * @return the keys contained in this counter map
   */
  Set<K> keySet();
}
