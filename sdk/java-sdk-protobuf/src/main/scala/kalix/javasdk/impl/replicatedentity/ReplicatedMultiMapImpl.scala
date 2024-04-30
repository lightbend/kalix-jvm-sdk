/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import java.util.{ Set => JSet }
import java.util.{ Collection => JCollection }
import java.util.{ Collections => JCollections }

import scala.jdk.CollectionConverters._

import kalix.javasdk.impl.AnySupport
import kalix.javasdk.replicatedentity.ReplicatedMultiMap
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.protocol.replicated_entity.ReplicatedMultiMapDelta
import kalix.protocol.replicated_entity.ReplicatedMultiMapEntryDelta

private[kalix] final class ReplicatedMultiMapImpl[K, V](
    anySupport: AnySupport,
    entries: Map[K, ReplicatedSetImpl[V]] = Map.empty[K, ReplicatedSetImpl[V]],
    removed: Set[K] = Set.empty[K],
    cleared: Boolean = false)
    extends ReplicatedMultiMap[K, V]
    with InternalReplicatedData {

  override type Self = ReplicatedMultiMapImpl[K, V]
  override val name = "ReplicatedMultiMap"

  /** for Scala SDK */
  def getValuesSet(key: K): Set[V] =
    entries.get(key).map(_.elementsSet).getOrElse(Set.empty[V])

  override def get(key: K): JSet[V] = entries.get(key).fold(JCollections.emptySet[V])(_.elements)

  override def put(key: K, value: V): ReplicatedMultiMapImpl[K, V] = {
    val values = entries.getOrElse(key, new ReplicatedSetImpl[V](anySupport))
    val updated = values.add(value)
    new ReplicatedMultiMapImpl(anySupport, entries.updated(key, updated), removed, cleared)
  }

  /** for Scala SDK */
  def putAll(key: K, values: Iterable[V]): ReplicatedMultiMapImpl[K, V] =
    values.foldLeft(this) { case (map, value) => map.put(key, value) }

  override def putAll(key: K, values: JCollection[V]): ReplicatedMultiMapImpl[K, V] =
    putAll(key, values.asScala)

  override def remove(key: K, value: V): ReplicatedMultiMapImpl[K, V] = {
    entries.get(key).fold(this) { values =>
      val updated = values.remove(value)
      if (updated.isEmpty) removeAll(key)
      else new ReplicatedMultiMapImpl(anySupport, entries.updated(key, updated), removed, cleared)
    }
  }

  override def removeAll(key: K): ReplicatedMultiMapImpl[K, V] = {
    if (!entries.contains(key)) {
      this
    } else {
      new ReplicatedMultiMapImpl(anySupport, entries.removed(key), removed + key, cleared)
    }
  }

  override def clear(): ReplicatedMultiMapImpl[K, V] =
    new ReplicatedMultiMapImpl[K, V](anySupport, cleared = true)

  override def size: Int = entries.values.map(_.size).sum

  override def isEmpty: Boolean = entries.isEmpty

  override def containsKey(key: K): Boolean = entries.contains(key)

  override def containsValue(key: K, value: V): Boolean =
    entries.get(key).fold(false)(_.contains(value))

  /** for Scala SDK */
  def keys: Set[K] = entries.keySet

  override def keySet: JSet[K] = entries.keySet.asJava

  override def hasDelta: Boolean = cleared || removed.nonEmpty || entries.values.exists(_.hasDelta)

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedMultiMap(
      ReplicatedMultiMapDelta(
        cleared = cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        updated = entries.collect {
          case (key, values) if values.hasDelta =>
            ReplicatedMultiMapEntryDelta(Some(anySupport.encodeScala(key)), values.getDelta.replicatedSet)
        }.toSeq))

  override def resetDelta(): ReplicatedMultiMapImpl[K, V] =
    if (hasDelta) new ReplicatedMultiMapImpl(anySupport, entries.view.mapValues(_.resetDelta()).toMap) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedMultiMapImpl[K, V]] = {
    case ReplicatedEntityDelta.Delta.ReplicatedMultiMap(ReplicatedMultiMapDelta(cleared, removed, updated, _)) =>
      val reducedEntries =
        if (cleared) Map.empty[K, ReplicatedSetImpl[V]]
        else entries -- removed.map(key => anySupport.decodePossiblyPrimitive(key).asInstanceOf[K])
      val updatedEntries = updated.foldLeft(reducedEntries) {
        case (map, ReplicatedMultiMapEntryDelta(Some(encodedKey), Some(delta), _)) =>
          val key = anySupport.decodePossiblyPrimitive(encodedKey).asInstanceOf[K]
          val values = map.getOrElse(key, new ReplicatedSetImpl[V](anySupport))
          map.updated(key, values.applyDelta(ReplicatedEntityDelta.Delta.ReplicatedSet(delta)))
        case (map, _) => map
      }
      new ReplicatedMultiMapImpl(anySupport, updatedEntries)
  }

  override def toString = s"ReplicatedMultiMap(${entries.map { case (k, v) => s"$k->$v" }.mkString(",")})"

}
