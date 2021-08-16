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

import java.util.Set;

/**
 * A Map of counters. Uses {@link ReplicatedCounter}'s as values.
 *
 * @param <K> The type for keys.
 */
public interface ReplicatedCounterMap<K> extends ReplicatedData {

  /**
   * Get the counter value for the given key.
   *
   * @param key The key to get the value for.
   * @return The current value of the counter at that key, or zero if no counter exists for that
   *     key.
   */
  long get(K key);

  /**
   * Increment the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key The key of the counter.
   * @param by The amount to increment by.
   * @return The new value of the counter.
   */
  long increment(K key, long by);

  /**
   * Decrement the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key The key of the counter.
   * @param by The amount to decrement by.
   * @return The new value of the counter.
   */
  long decrement(K key, long by);

  Set<K> keySet();

  int size();

  boolean isEmpty();

  boolean containsKey(K key);

  void remove(K key);

  void clear();
}
