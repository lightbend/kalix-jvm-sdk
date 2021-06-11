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

import java.util.Map;

/**
 * A Map of counters. Uses {@link ReplicatedCounter}'s as values.
 *
 * @param <K> The type for keys.
 */
public final class ReplicatedCounterMap<K> extends AbstractORMapWrapper<K, Long, ReplicatedCounter>
    implements Map<K, Long> {

  public ReplicatedCounterMap(ORMap<K, ReplicatedCounter> ormap) {
    super(ormap);
  }

  /**
   * Get the counter value for the given key.
   *
   * @param key The key to get the value for.
   * @return The current value of the counter at that key, or zero if no counter exists for that
   *     key.
   */
  public long getValue(Object key) {
    ReplicatedCounter counter = ormap.get(key);
    if (counter != null) {
      return counter.getValue();
    } else {
      return 0;
    }
  }

  /**
   * Increment the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key The key of the counter.
   * @param by The amount to increment by.
   * @return The new value of the counter.
   */
  public long increment(Object key, long by) {
    return getOrUpdate(key).increment(by);
  }

  /**
   * Decrement the counter at the given key by the given amount.
   *
   * <p>The counter will be created if it is not already in the map.
   *
   * @param key The key of the counter.
   * @param by The amount to decrement by.
   * @return The new value of the counter.
   */
  public long decrement(Object key, long by) {
    return getOrUpdate(key).decrement(by);
  }

  /** Not supported on PNCounter, use increment/decrement instead. */
  @Override
  public Long put(K key, Long value) {
    throw new UnsupportedOperationException(
        "Put is not supported on PNCounterMap, use increment or decrement instead");
  }

  @Override
  Long getValue(ReplicatedCounter counter) {
    return counter.getValue();
  }

  @Override
  void setValue(ReplicatedCounter counter, Long value) {
    throw new UnsupportedOperationException(
        "Using value mutating methods on PNCounterMap is not supported, use increment or decrement instead");
  }

  @Override
  ReplicatedCounter getOrUpdateEntity(K key, Long value) {
    return ormap.getOrCreate(key, ReplicatedDataFactory::newCounter);
  }

  private ReplicatedCounter getOrUpdate(Object key) {
    return ormap.getOrCreate((K) key, ReplicatedDataFactory::newCounter);
  }
}
