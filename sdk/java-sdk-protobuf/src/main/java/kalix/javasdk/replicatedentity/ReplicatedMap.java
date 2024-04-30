/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import kalix.replicatedentity.ReplicatedData;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

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

  /**
   * Get a {@link ReplicatedCounter} from a heterogeneous Replicated Map (a map with different types
   * of Replicated Data values).
   *
   * @param key the key for a Replicated Counter in this map
   * @return the {@link ReplicatedCounter} associated with the given key, or an empty counter
   */
  @SuppressWarnings("unchecked")
  default ReplicatedCounter getReplicatedCounter(K key) {
    return (ReplicatedCounter) getOrElse(key, factory -> (V) factory.newCounter());
  }

  /**
   * Get a {@link ReplicatedRegister} from a heterogeneous Replicated Map (a map with different
   * types of Replicated Data values).
   *
   * @param key the key for a Replicated Register in this map
   * @return the {@link ReplicatedRegister} associated with the given key, or an empty register
   * @param <ValueT> the value type for the Replicated Register
   */
  default <ValueT> ReplicatedRegister<ValueT> getReplicatedRegister(K key) {
    return getReplicatedRegister(key, () -> null);
  }

  /**
   * Get a {@link ReplicatedRegister} from a heterogeneous Replicated Map (a map with different
   * types of Replicated Data values).
   *
   * @param key the key for a Replicated Register in this map
   * @param defaultValue the supplier for a default value when the register is not present
   * @return the {@link ReplicatedRegister} associated with the given key, or a default register
   * @param <ValueT> the value type for the Replicated Register
   */
  @SuppressWarnings("unchecked")
  default <ValueT> ReplicatedRegister<ValueT> getReplicatedRegister(
      K key, Supplier<ValueT> defaultValue) {
    return (ReplicatedRegister<ValueT>)
        getOrElse(key, factory -> (V) factory.newRegister(defaultValue.get()));
  }

  /**
   * Get a {@link ReplicatedSet} from a heterogeneous Replicated Map (a map with different types of
   * Replicated Data values).
   *
   * @param key the key for a Replicated Set in this map
   * @return the {@link ReplicatedSet} associated with the given key, or an empty set
   * @param <ElementT> the element type for the Replicated Set
   */
  @SuppressWarnings("unchecked")
  default <ElementT> ReplicatedSet<ElementT> getReplicatedSet(K key) {
    return (ReplicatedSet<ElementT>)
        getOrElse(key, factory -> (V) factory.<ElementT>newReplicatedSet());
  }

  /**
   * Get a {@link ReplicatedCounterMap} from a heterogeneous Replicated Map (a map with different
   * types of Replicated Data values).
   *
   * @param key the key for a Replicated Counter Map in this map
   * @return the {@link ReplicatedCounterMap} associated with the given key, or an empty map
   * @param <KeyT> the key type for the Replicated Counter Map
   */
  @SuppressWarnings("unchecked")
  default <KeyT> ReplicatedCounterMap<KeyT> getReplicatedCounterMap(K key) {
    return (ReplicatedCounterMap<KeyT>)
        getOrElse(key, factory -> (V) factory.<KeyT>newReplicatedCounterMap());
  }

  /**
   * Get a {@link ReplicatedRegisterMap} from a heterogeneous Replicated Map (a map with different
   * types of Replicated Data values).
   *
   * @param key the key for a Replicated Register Map in this map
   * @return the {@link ReplicatedRegisterMap} associated with the given key, or an empty map
   * @param <KeyT> the key type for the Replicated Register Map
   * @param <ValueT> the value type for the Replicated Register Map
   */
  @SuppressWarnings("unchecked")
  default <KeyT, ValueT> ReplicatedRegisterMap<KeyT, ValueT> getReplicatedRegisterMap(K key) {
    return (ReplicatedRegisterMap<KeyT, ValueT>)
        getOrElse(key, factory -> (V) factory.<KeyT, ValueT>newReplicatedRegisterMap());
  }

  /**
   * Get a {@link ReplicatedMultiMap} from a heterogeneous Replicated Map (a map with different
   * types of Replicated Data values).
   *
   * @param key the key for a Replicated Multi-Map in this map
   * @return the {@link ReplicatedMultiMap} associated with the given key, or an empty multi-map
   * @param <KeyT> the key type for the Replicated Multi-Map
   * @param <ValueT> the value type for the Replicated Multi-Map
   */
  @SuppressWarnings("unchecked")
  default <KeyT, ValueT> ReplicatedMultiMap<KeyT, ValueT> getReplicatedMultiMap(K key) {
    return (ReplicatedMultiMap<KeyT, ValueT>)
        getOrElse(key, factory -> (V) factory.<KeyT, ValueT>newReplicatedMultiMap());
  }

  /**
   * Get a {@link ReplicatedMap} from a heterogeneous Replicated Map (a map with different types of
   * Replicated Data values).
   *
   * @param key the key for a Replicated Map in this map
   * @return the {@link ReplicatedMap} associated with the given key, or an empty map
   * @param <KeyT> the key type for the Replicated Map
   * @param <ValueT> the value type for the Replicated Map
   */
  @SuppressWarnings("unchecked")
  default <KeyT, ValueT extends ReplicatedData> ReplicatedMap<KeyT, ValueT> getReplicatedMap(
      K key) {
    return (ReplicatedMap<KeyT, ValueT>)
        getOrElse(key, factory -> (V) factory.<KeyT, ValueT>newReplicatedMap());
  }
}
