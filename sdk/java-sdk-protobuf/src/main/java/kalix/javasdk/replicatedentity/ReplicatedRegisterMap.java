/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import java.util.Optional;
import java.util.Set;
import kalix.replicatedentity.ReplicatedData;

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
   * @param key the key for the register
   * @return the current value of the register, if it exists (as an Optional)
   */
  Optional<V> getValue(K key);

  /**
   * Set the current value of the register at the given key, using the default clock.
   *
   * @param key the key for the register
   * @param value the new value of the register
   * @return a new register map with the updated value
   */
  default ReplicatedRegisterMap<K, V> setValue(K key, V value) {
    return setValue(key, value, ReplicatedRegister.Clock.DEFAULT, 0);
  }

  /**
   * Set the current value of the register at the given key, using the given clock and custom clock
   * value if required.
   *
   * @param key the key for the register
   * @param value the new value of the register
   * @param clock the clock to use for replication
   * @param customClockValue the custom clock value to use, only if it's a custom clock
   * @return a new register map with the updated value
   */
  ReplicatedRegisterMap<K, V> setValue(
      K key, V value, ReplicatedRegister.Clock clock, long customClockValue);

  /**
   * Remove the mapping for a key if it is present.
   *
   * @param key key whose mapping is to be removed from the map
   * @return a new register map with the removed mapping
   */
  ReplicatedRegisterMap<K, V> remove(K key);

  /**
   * Remove all mappings from this register map.
   *
   * @return a new empty register map
   */
  ReplicatedRegisterMap<K, V> clear();

  /**
   * Get the number of key-register mappings in this register map.
   *
   * @return the number of key-register mappings in this register map
   */
  int size();

  /**
   * Check whether this register map is empty.
   *
   * @return {@code true} if this register map contains no key-register mappings
   */
  boolean isEmpty();

  /**
   * Check whether this register map contains a mapping for the given key.
   *
   * @param key key whose presence in this map is to be tested
   * @return {@code true} if this register map contains a mapping for the given key
   */
  boolean containsKey(K key);

  /**
   * Get a {@link Set} view of the keys contained in this register map.
   *
   * @return the keys contained in this register map
   */
  Set<K> keySet();
}
