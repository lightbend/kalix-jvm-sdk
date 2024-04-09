/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import kalix.javasdk.impl.replicatedentity.ReplicatedMapImpl
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.impl.replicatedentity.ScalaReplicatedDataConverter
import kalix.scalasdk.impl.replicatedentity.ScalaReplicatedDataFactoryAdapter

/**
 * A Replicated Map that allows both the addition and removal of [[kalix.replicatedentity.ReplicatedData]] objects.
 *
 * Use the more specialized maps if possible, such as [[kalix.scalasdk.replicatedentity.ReplicatedCounterMap]],
 * [[kalix.scalasdk.replicatedentity.ReplicatedRegisterMap]], and
 * [[kalix.scalasdk.replicatedentity.ReplicatedMultiMap]].
 *
 * A removal can only be done if all of the additions that caused the key to be in the map have been seen by this node.
 * This means that, for example, if node 1 adds key A, and node 2 also adds key A, then node 1's addition is replicated
 * to node 3, and node 3 deletes it before node 2's addition is replicated, then the item will still be in the map
 * because node 2's addition had not yet been observed by node 3. However, if both additions had been replicated to node
 * 3, then the key will be removed.
 *
 * The values of the map are themselves [[kalix.replicatedentity.ReplicatedData]] types, and hence allow concurrent
 * updates that will eventually converge. New [[kalix.replicatedentity.ReplicatedData]] objects may only be created when
 * using the [[kalix.scalasdk.replicatedentity.ReplicatedMap.getOrElse*]] method, using the provided
 * [[kalix.scalasdk.replicatedentity.ReplicatedDataFactory]] for the create function.
 *
 * While removing entries from the map is supported, if the entries are added back again, it is possible that the value
 * of the deleted entry may be merged into the value of the current entry, depending on whether the removal has been
 * replicated to all nodes before the addition is performed.
 *
 * The map may contain different data types as values, however, for a given key, the type must never change. If two
 * different types for the same key are inserted on different nodes, the replicated entity will enter an invalid state
 * that can never be merged, and behavior of the replicated entity is undefined.
 *
 * Care needs to be taken to ensure that the serialized value of keys in the set is stable. For example, if using
 * protobufs, the serialized value of any maps contained in the protobuf is not stable, and can yield a different set of
 * bytes for the same logically equal element. Hence maps should be avoided. Additionally, some changes in protobuf
 * schemas which are backwards compatible from a protobuf perspective, such as changing from sint32 to int32, do result
 * in different serialized bytes, and so must be avoided.
 *
 * @tparam K
 *   The type of keys.
 * @tparam V
 *   The replicated data type to be used for values.
 */
class ReplicatedMap[K, V <: ReplicatedData] private[scalasdk] (override val delegate: ReplicatedMapImpl[K, V])
    extends InternalReplicatedData {

  /**
   * Get the [[kalix.replicatedentity.ReplicatedData]] value for the given key.
   *
   * @param key
   *   the key of the mapping
   * @return
   *   the [[kalix.replicatedentity.ReplicatedData]] for the key
   * @throws java.util.NoSuchElementException
   *   if the key is not preset in the map
   */
  def apply(key: K): V = delegate(key)

  /**
   * Optionally returns the [[kalix.replicatedentity.ReplicatedData]] value for the given key.
   *
   * @param key
   *   the key of the mapping
   * @return
   *   an option value containing the value associated with `key` in this
   *   [[kalix.scalasdk.replicatedentity.ReplicatedMap]], or `None` if none exists.
   */
  def get(key: K): Option[V] = delegate.getOption(key)

  /**
   * Get the [[kalix.replicatedentity.ReplicatedData]] value for the given key. If the key is not present in the map,
   * then a new value is created with a creation function.
   *
   * @param key
   *   the key of the mapping
   * @param create
   *   function used to create an empty value using the given [[kalix.scalasdk.replicatedentity.ReplicatedDataFactory]]
   *   if the key is not present in the map
   * @return
   *   the [[kalix.replicatedentity.ReplicatedData]] for the key
   */
  def getOrElse[ValueT <: ReplicatedData](key: K, create: ReplicatedDataFactory => ValueT): ValueT = {
    ScalaReplicatedDataConverter
      .convert(
        delegate
          .getOrElse(key, factory => create(ScalaReplicatedDataFactoryAdapter(factory)).asInstanceOf[V]))
      .asInstanceOf[ValueT]
  }

  /**
   * Update the [[kalix.replicatedentity.ReplicatedData]] value associated with the given key.
   *
   * @param key
   *   the key of the mapping
   * @param value
   *   the updated [[kalix.replicatedentity.ReplicatedData]] value
   * @return
   *   a new map with the updated value
   */
  def update(key: K, value: V): ReplicatedMap[K, V] =
    new ReplicatedMap(delegate.update(key, value))

  /**
   * Remove the mapping for a key if it is present.
   *
   * @param key
   *   key whose mapping is to be removed from the map
   * @return
   *   a new map with the removed mapping
   */
  def remove(key: K): ReplicatedMap[K, V] =
    new ReplicatedMap(delegate.remove(key))

  /**
   * Remove all entries from this map.
   *
   * @return
   *   a new empty map
   */
  def clear(): ReplicatedMap[K, V] =
    new ReplicatedMap(delegate.clear())

  /**
   * Get the number of key-value mappings in this map.
   *
   * @return
   *   the number of key-value mappings in this map
   */
  def size: Int = delegate.size

  /**
   * Check whether this map is empty.
   *
   * @return
   *   `true` if this map contains no key-value mappings
   */
  def isEmpty: Boolean = delegate.isEmpty

  /**
   * Check whether this map contains a mapping for the given key.
   *
   * @param key
   *   key whose presence in this map is to be tested
   * @return
   *   `true` if this map contains a mapping for the given key
   */
  def contains(key: K): Boolean = delegate.containsKey(key)

  /**
   * Get a [[scala.collection.immutable.Set]] view of the keys contained in this map.
   *
   * @return
   *   the keys contained in this map
   */
  def keySet: Set[K] =
    delegate.keys

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedCounter]] from a heterogeneous Replicated Map (a map with
   * different types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Counter in this map
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedCounter]] associated with the given key, or an empty counter
   */
  def getReplicatedCounter(key: K): ReplicatedCounter =
    getOrElse(key, factory => factory.newCounter)

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedRegister]] from a heterogeneous Replicated Map (a map with
   * different types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Register in this map
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedRegister]] associated with the given key, or an empty register
   * @tparam ValueT
   *   the value type for the Replicated Register
   */
  def getReplicatedRegister[ValueT <: AnyRef](key: K): ReplicatedRegister[ValueT] =
    getReplicatedRegister(key, () => null.asInstanceOf[ValueT])

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedRegister]] from a heterogeneous Replicated Map (a map with
   * different types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Register in this map
   * @param defaultValue
   *   the supplier for a default value when the register is not present
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedRegister]] associated with the given key, or a default register
   * @tparam ValueT
   *   the value type for the Replicated Register
   */
  def getReplicatedRegister[ValueT](key: K, defaultValue: () => ValueT): ReplicatedRegister[ValueT] =
    getOrElse(key, factory => factory.newRegister(defaultValue()))

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedSet]] from a heterogeneous Replicated Map (a map with different
   * types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Set in this map
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedSet]] associated with the given key, or an empty set
   * @tparam ElementT
   *   the element type for the Replicated Set
   */
  def getReplicatedSet[ElementT](key: K): ReplicatedSet[ElementT] =
    getOrElse(key, factory => factory.newReplicatedSet)

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedCounterMap]] from a heterogeneous Replicated Map (a map with
   * different types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Counter Map in this map
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedCounterMap]] associated with the given key, or an empty map
   * @tparam KeyT
   *   the key type for the Replicated Counter Map
   */
  def getReplicatedCounterMap[KeyT](key: K): ReplicatedCounterMap[KeyT] =
    getOrElse(key, factory => factory.newReplicatedCounterMap)

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedRegisterMap]] from a heterogeneous Replicated Map (a map with
   * different types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Register Map in this map
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedRegisterMap]] associated with the given key, or an empty map
   * @tparam KeyT
   *   the key type for the Replicated Register Map
   * @tparam ValueT
   *   the value type for the Replicated Register Map
   */
  def getReplicatedRegisterMap[KeyT, ValueT](key: K): ReplicatedRegisterMap[KeyT, ValueT] =
    getOrElse(key, factory => factory.newReplicatedRegisterMap)

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedMultiMap]] from a heterogeneous Replicated Map (a map with
   * different types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Multi-Map in this map
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedMultiMap]] associated with the given key, or an empty multi-map
   * @tparam KeyT
   *   the key type for the Replicated Multi-Map
   * @tparam ValueT
   *   the value type for the Replicated Multi-Map
   */
  def getReplicatedMultiMap[KeyT, ValueT](key: K): ReplicatedMultiMap[KeyT, ValueT] =
    getOrElse(key, factory => factory.newReplicatedMultiMap)

  /**
   * Get a [[kalix.scalasdk.replicatedentity.ReplicatedMap]] from a heterogeneous Replicated Map (a map with different
   * types of Replicated Data values).
   *
   * @param key
   *   the key for a Replicated Map in this map
   * @return
   *   the [[kalix.scalasdk.replicatedentity.ReplicatedMap]] associated with the given key, or an empty map
   * @tparam KeyT
   *   the key type for the Replicated Map
   * @tparam ValueT
   *   the value type for the Replicated Map
   */
  def getReplicatedMap[KeyT, ValueT <: ReplicatedData](key: K): ReplicatedMap[KeyT, ValueT] =
    getOrElse(key, factory => factory.newReplicatedMap)

  final override type Self = ReplicatedMap[K, V]

  final override def resetDelta(): ReplicatedMap[K, V] =
    new ReplicatedMap(delegate.resetDelta())

  final override def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedMap[K, V]] =
    delegate.applyDelta.andThen(new ReplicatedMap(_))
}
