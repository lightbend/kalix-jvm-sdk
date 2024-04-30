/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import kalix.javasdk.impl.AnySupport
import kalix.javasdk.replicatedentity.ReplicatedRegister
import kalix.javasdk.replicatedentity.ReplicatedRegisterMap
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.protocol.replicated_entity.ReplicatedRegisterMapDelta
import kalix.protocol.replicated_entity.ReplicatedRegisterMapEntryDelta

private[kalix] final class ReplicatedRegisterMapImpl[K, V](
    anySupport: AnySupport,
    registers: Map[K, ReplicatedRegisterImpl[V]] = Map.empty[K, ReplicatedRegisterImpl[V]],
    removed: Set[K] = Set.empty[K],
    cleared: Boolean = false)
    extends ReplicatedRegisterMap[K, V]
    with InternalReplicatedData {

  override type Self = ReplicatedRegisterMapImpl[K, V]
  override val name = "ReplicatedRegisterMap"

  /** for Scala SDK */
  def getValueOption(key: K): Option[V] =
    registers.get(key).map(_.get())

  override def getValue(key: K): java.util.Optional[V] =
    getValueOption(key).toJava

  override def setValue(
      key: K,
      value: V,
      clock: ReplicatedRegister.Clock,
      customClockValue: Long): ReplicatedRegisterMapImpl[K, V] = {
    val register = registers.getOrElse(key, new ReplicatedRegisterImpl[V](anySupport))
    val updated = register.set(value, clock, customClockValue)
    new ReplicatedRegisterMapImpl(anySupport, registers.updated(key, updated), removed, cleared)
  }

  override def remove(key: K): ReplicatedRegisterMapImpl[K, V] = {
    if (!registers.contains(key)) {
      this
    } else {
      new ReplicatedRegisterMapImpl(anySupport, registers.removed(key), removed + key, cleared)
    }
  }

  override def clear(): ReplicatedRegisterMapImpl[K, V] =
    new ReplicatedRegisterMapImpl[K, V](anySupport, cleared = true)

  override def size: Int = registers.size

  override def isEmpty: Boolean = registers.isEmpty

  override def containsKey(key: K): Boolean = registers.contains(key)

  /** for Scala SDK */
  def keys: Set[K] = registers.keySet

  override def keySet: java.util.Set[K] = keys.asJava

  override def hasDelta: Boolean = cleared || removed.nonEmpty || registers.values.exists(_.hasDelta)

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedRegisterMap(
      ReplicatedRegisterMapDelta(
        cleared = cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        updated = registers.collect {
          case (key, register) if register.hasDelta =>
            ReplicatedRegisterMapEntryDelta(Some(anySupport.encodeScala(key)), register.getDelta.register)
        }.toSeq))

  override def resetDelta(): ReplicatedRegisterMapImpl[K, V] =
    if (hasDelta) new ReplicatedRegisterMapImpl(anySupport, registers.view.mapValues(_.resetDelta()).toMap) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedRegisterMapImpl[K, V]] = {
    case ReplicatedEntityDelta.Delta.ReplicatedRegisterMap(ReplicatedRegisterMapDelta(cleared, removed, updated, _)) =>
      val reducedRegisters =
        if (cleared) Map.empty[K, ReplicatedRegisterImpl[V]]
        else registers -- removed.map(key => anySupport.decodePossiblyPrimitive(key).asInstanceOf[K])
      val updatedRegisters = updated.foldLeft(reducedRegisters) {
        case (map, ReplicatedRegisterMapEntryDelta(Some(encodedKey), Some(delta), _)) =>
          val key = anySupport.decodePossiblyPrimitive(encodedKey).asInstanceOf[K]
          val register = map.getOrElse(key, new ReplicatedRegisterImpl[V](anySupport))
          map.updated(key, register.applyDelta(ReplicatedEntityDelta.Delta.Register(delta)))
        case (map, _) => map
      }
      new ReplicatedRegisterMapImpl(anySupport, updatedRegisters)
  }

  override def toString = s"ReplicatedRegisterMap(${registers.map { case (k, v) => s"$k->$v" }.mkString(",")})"

}
