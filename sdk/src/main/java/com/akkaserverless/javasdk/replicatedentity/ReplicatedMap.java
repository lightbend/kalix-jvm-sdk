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

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

/**
 * A Replicated Map that allows both the addition and removal of {@link ReplicatedData} objects.
 *
 * <p>Use the more specialized maps if possible, such as {@link ReplicatedCounterMap}, {@link
 * ReplicatedRegisterMap}, and {@link ReplicatedMultiMap}.
 *
 * <p>A removal can only be done if all of the additions that caused the key to be in the map have
 * been seen by this node. This means that, for example, if node 1 adds key A, and node 2 also adds
 * key A, then node 1's addition is replicated to node 3, and node 3 deletes it before node 2's
 * addition is replicated, then the item will still be in the map because node 2's addition had not
 * yet been observed by node 3. However, if both additions had been replicated to node 3, then the
 * key will be removed.
 *
 * <p>The values of the map are themselves {@link ReplicatedData} types, and hence allow concurrent
 * updates that will eventually converge. New {@link ReplicatedData} objects may only be created
 * when using the {@link ReplicatedMap#getOrElse(Object, Function)} method, using the provided
 * {@link ReplicatedDataFactory} for the create function.
 *
 * <p>While removing entries from the map is supported, if the entries are added back again, it is
 * possible that the value of the deleted entry may be merged into the value of the current entry,
 * depending on whether the removal has been replicated to all nodes before the addition is
 * performed.
 *
 * <p>The map may contain different data types as values, however, for a given key, the type must
 * never change. If two different types for the same key are inserted on different nodes, the
 * replicated entity will enter an invalid state that can never be merged, and behavior of the
 * replicated entity is undefined.
 *
 * <p>Care needs to be taken to ensure that the serialized value of keys in the set is stable. For
 * example, if using protobufs, the serialized value of any maps contained in the protobuf is not
 * stable, and can yield a different set of bytes for the same logically equal element. Hence maps
 * should be avoided. Additionally, some changes in protobuf schemas which are backwards compatible
 * from a protobuf perspective, such as changing from sint32 to int32, do result in different
 * serialized bytes, and so must be avoided.
 *
 * @param <K> The type of keys.
 * @param <V> The replicated data type to be used for values.
 */
public interface ReplicatedMap<K, V extends ReplicatedData> extends ReplicatedData {

  /**
   * Get the {@link ReplicatedData} value for the given key.
   *
   * @param key the key of the mapping
   * @return the {@link ReplicatedData} for the key
   * @throws NoSuchElementException if the key is not preset in the map
   */
  V get(K key);

  /**
   * Get the {@link ReplicatedData} value for the given key. If the key is not present in the map,
   * then a new value is created with a creation function.
   *
   * @param key the key of the mapping
   * @param create function used to create an empty value using the given {@link
   *     ReplicatedDataFactory} if the key is not present in the map
   * @return the {@link ReplicatedData} for the key
   */
  V getOrElse(K key, Function<ReplicatedDataFactory, V> create);

  /**
   * Update the {@link ReplicatedData} value associated with the given key.
   *
   * @param key the key of the mapping
   * @param value the updated {@link ReplicatedData} value
   * @return a new map with the updated value
   */
  ReplicatedMap<K, V> update(K key, V value);

  /**
   * Remove the mapping for a key if it is present.
   *
   * @param key key whose mapping is to be removed from the map
   * @return a new map with the removed mapping
   */
  ReplicatedMap<K, V> remove(K key);

  /**
   * Remove all entries from this map.
   *
   * @return a new empty map
   */
  ReplicatedMap<K, V> clear();

  /**
   * Get the number of key-value mappings in this map.
   *
   * @return the number of key-value mappings in this map
   */
  int size();

  /**
   * Check whether this map is empty.
   *
   * @return {@code true} if this map contains no key-value mappings
   */
  boolean isEmpty();

  /**
   * Check whether this map contains a mapping for the given key.
   *
   * @param key key whose presence in this map is to be tested
   * @return {@code true} if this map contains a mapping for the given key
   */
  boolean containsKey(K key);

  /**
   * Get a {@link Set} view of the keys contained in this map.
   *
   * @return the keys contained in this map
   */
  Set<K> keySet();
}
