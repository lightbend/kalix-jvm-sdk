/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import kalix.javasdk.impl.replicatedentity.ReplicatedRegisterMapImpl
import kalix.javasdk.replicatedentity.{ ReplicatedRegister => JavaSdkReplicatedRegister }
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

/**
 * A Map of registers. Uses [[ReplicatedRegister]] 's as values.
 *
 * @tparam K
 *   The type for keys.
 * @tparam V
 *   The type for values.
 */
class ReplicatedRegisterMap[K, V] private[scalasdk] (override val delegate: ReplicatedRegisterMapImpl[K, V])
    extends InternalReplicatedData {

  /**
   * Optionally returns the current value of the register at the given key.
   *
   * @param key
   *   the key for the register
   * @return
   *   the current value of the register, if it exists (as an Option)
   */
  def get(key: K): Option[V] = delegate.getValueOption(key)

  /**
   * Get the register value for the given key.
   *
   * @param key
   *   the key for the register
   * @return
   *   the register value for the key
   * @throws java.util.NoSuchElementException
   *   if the key is not preset in the map
   */
  def apply(key: K): V = get(key).get

  /**
   * Set the current value of the register at the given key, using the default clock.
   *
   * @param key
   *   the key for the register
   * @param value
   *   the new value of the register
   * @return
   *   a new register map with the updated value
   */
  def setValue(key: K, value: V): ReplicatedRegisterMap[K, V] =
    new ReplicatedRegisterMap(delegate.setValue(key, value, JavaSdkReplicatedRegister.Clock.DEFAULT, 0))

  /**
   * Set the current value of the register at the given key, using the given clock and custom clock value if required.
   *
   * @param key
   *   the key for the register
   * @param value
   *   the new value of the register
   * @param clock
   *   the clock to use for replication
   * @param customClockValue
   *   the custom clock value to use, only if it's a custom clock
   * @return
   *   a new register map with the updated value
   */
  def setValue(
      key: K,
      value: V,
      clock: ReplicatedRegister.Clock,
      customClockValue: Long): ReplicatedRegisterMap[K, V] = {
    val javaClock =
      clock match {
        case ReplicatedRegister.Default             => JavaSdkReplicatedRegister.Clock.DEFAULT
        case ReplicatedRegister.Reverse             => JavaSdkReplicatedRegister.Clock.REVERSE
        case ReplicatedRegister.Custom              => JavaSdkReplicatedRegister.Clock.CUSTOM
        case ReplicatedRegister.CustomAutoIncrement => JavaSdkReplicatedRegister.Clock.CUSTOM_AUTO_INCREMENT
      }
    new ReplicatedRegisterMap(delegate.setValue(key, value, javaClock, customClockValue))
  }

  /**
   * Remove the mapping for a key if it is present.
   *
   * @param key
   *   key whose mapping is to be removed from the map
   * @return
   *   a new register map with the removed mapping
   */
  def remove(key: K): ReplicatedRegisterMap[K, V] =
    new ReplicatedRegisterMap(delegate.remove(key))

  /**
   * Remove all mappings from this register map.
   *
   * @return
   *   a new empty register map
   */
  def clear(): ReplicatedRegisterMap[K, V] =
    new ReplicatedRegisterMap(delegate.clear())

  /**
   * Get the number of key-register mappings in this register map.
   *
   * @return
   *   the number of key-register mappings in this register map
   */
  def size: Int = delegate.size

  /**
   * Check whether this register map is empty.
   *
   * @return
   *   `true` if this register map contains no key-register mappings
   */
  def isEmpty: Boolean = delegate.isEmpty

  /**
   * Check whether this register map contains a mapping for the given key.
   *
   * @param key
   *   key whose presence in this map is to be tested
   * @return
   *   `true` if this register map contains a mapping for the given key
   */
  def containsKey(key: K): Boolean = delegate.containsKey(key)

  /**
   * Get a [[scala.collection.immutable.Set]] view of the keys contained in this register map.
   *
   * @return
   *   the keys contained in this register map
   */
  def keySet: Set[K] = delegate.keys

  final override type Self = ReplicatedRegisterMap[K, V]

  final override def resetDelta(): ReplicatedRegisterMap[K, V] =
    new ReplicatedRegisterMap(delegate.resetDelta())

  final override def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedRegisterMap[K, V]] =
    delegate.applyDelta.andThen(new ReplicatedRegisterMap(_))
}
