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

import java.util.Optional;
import java.util.Set;

/**
 * A Map of registers. Uses {@link ReplicatedRegister}'s as values.
 *
 * @param <K> The type for keys.
 * @param <V> The type for values.
 */
public interface ReplicatedRegisterMap<K, V> extends ReplicatedData {

  /**
   * Get the current value of the register at the given key.
   *
   * @param key The key for the register.
   * @return The current value of the register, if it exists (as an Optional).
   */
  Optional<V> getValue(K key);

  /**
   * Set the current value of the register at the given key, using the default clock.
   *
   * @param key The key for the register.
   * @param value The value of the register to set.
   */
  default void setValue(K key, V value) {
    setValue(key, value, ReplicatedRegister.Clock.DEFAULT, 0);
  }

  /**
   * Set the current value of the register at the given key, using the given clock and custom clock
   * value if required.
   *
   * @param key The key for the register.
   * @param value The value of the register to set.
   * @param clock The clock to use.
   * @param customClockValue The custom clock value to use if the clock selected is a custom clock.
   *     This is ignored if the clock is not a custom clock.
   */
  void setValue(K key, V value, ReplicatedRegister.Clock clock, long customClockValue);

  Set<K> keySet();

  int size();

  boolean isEmpty();

  boolean containsKey(K key);

  void remove(K key);

  void clear();
}
