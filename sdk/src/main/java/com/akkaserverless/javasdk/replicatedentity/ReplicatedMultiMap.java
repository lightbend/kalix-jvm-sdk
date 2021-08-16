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

package com.akkaserverless.javasdk.replicatedentity;

import java.util.Collection;
import java.util.Set;

/**
 * A replicated map that maps keys to values, where each key may be associated with multiple values.
 * Effectively a ReplicatedMap with {@link ReplicatedSet}s as values.
 *
 * @param <K> The type for keys.
 * @param <V> The type for values.
 */
public interface ReplicatedMultiMap<K, V> extends ReplicatedData {

  /**
   * Get the values for the given key.
   *
   * @param key The key of the entry.
   * @return The current values at the given key, or an empty Set.
   */
  Set<V> get(K key);

  /**
   * Store a key-value pair.
   *
   * @param key The key of the entry.
   * @param value The value to add to the entry.
   * @return {@code true} if the multimap changed (this key-value pair was added).
   */
  boolean put(K key, V value);

  /**
   * Store multiple values for a key.
   *
   * @param key The key of the entry.
   * @param values The values to add to the entry.
   * @return {@code true} if the multimap changed (values were added).
   */
  boolean putAll(K key, Collection<V> values);

  /**
   * Remove a single key-value pair for the given key and value.
   *
   * @param key The key of the entry.
   * @param value The value to remove from the entry.
   * @return {@code true} if the multimap changed (this key-value pair was removed).
   */
  boolean remove(K key, V value);

  /**
   * Remove all values associated with the given key.
   *
   * @param key The key of the entry.
   * @return {@code true} if the multimap changed (values were removed).
   */
  boolean removeAll(K key);

  /**
   * Return the keys contained in this multimap.
   *
   * <p>Note that the key set contains a key if and only if this multimap maps that key to at least
   * one value.
   *
   * @return the set of keys in this multimap.
   */
  Set<K> keySet();

  /**
   * Return the number of key-value pairs in this multimap.
   *
   * <p>Note that this does not return the number of distinct keys, which is given by {@code
   * keySet().size()}, but the total number of values stored in the multimap.
   *
   * @return the number of key-value pairs stored in this multimap.
   */
  int size();

  /**
   * Check whether this multimap is empty.
   *
   * @return {@code true} if this multimap contains no key-value pairs.
   */
  boolean isEmpty();

  /**
   * Check whether this multimap contains at least one value for the given key.
   *
   * @param key The key of the entry.
   * @return {@code true} if there is at least one key-value pair with the key.
   */
  boolean containsKey(K key);

  /**
   * Check whether this multimap contains the given value associated with the given key.
   *
   * @param key The key of the entry.
   * @param value The value of the entry.
   * @return {@code true} if the key-value pair is in this multimap.
   */
  boolean containsValue(K key, V value);

  /** Remove all key-value pairs from the multimap, leaving it empty. */
  void clear();
}
