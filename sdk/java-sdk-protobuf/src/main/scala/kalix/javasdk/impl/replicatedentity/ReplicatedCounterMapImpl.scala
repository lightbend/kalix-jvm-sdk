/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import scala.jdk.CollectionConverters._

import kalix.javasdk.impl.AnySupport
import kalix.javasdk.replicatedentity.ReplicatedCounterMap
import kalix.protocol.replicated_entity.ReplicatedCounterMapDelta
import kalix.protocol.replicated_entity.ReplicatedCounterMapEntryDelta
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

private[kalix] final class ReplicatedCounterMapImpl[K](
    anySupport: AnySupport,
    counters: Map[K, ReplicatedCounterImpl] = Map.empty[K, ReplicatedCounterImpl],
    removed: Set[K] = Set.empty[K],
    cleared: Boolean = false)
    extends ReplicatedCounterMap[K]
    with InternalReplicatedData {

  override type Self = ReplicatedCounterMapImpl[K]
  override val name = "ReplicatedCounterMap"

  /** for Scala SDK */
  def getOption(key: K): Option[Long] =
    counters.get(key).map(_.getValue)

  override def get(key: K): Long =
    counters.get(key).fold(0L)(_.getValue)

  override def increment(key: K, amount: Long): ReplicatedCounterMapImpl[K] = {
    val counter = counters.getOrElse(key, new ReplicatedCounterImpl)
    val incremented = counter.increment(amount)
    new ReplicatedCounterMapImpl(anySupport, counters.updated(key, incremented), removed, cleared)
  }

  override def decrement(key: K, amount: Long): ReplicatedCounterMapImpl[K] = increment(key, -amount)

  override def remove(key: K): ReplicatedCounterMapImpl[K] = {
    if (!counters.contains(key)) {
      this
    } else {
      new ReplicatedCounterMapImpl(anySupport, counters.removed(key), removed + key, cleared)
    }
  }

  override def clear(): ReplicatedCounterMapImpl[K] =
    new ReplicatedCounterMapImpl[K](anySupport, cleared = true)

  override def size: Int = counters.size

  override def isEmpty: Boolean = counters.isEmpty

  /** for Scala SDK */
  def forall(predicate: ((K, Long)) => Boolean): Boolean =
    counters.view.mapValues(_.getValue).forall(predicate)

  override def containsKey(key: K): Boolean = counters.contains(key)

  override def keySet: java.util.Set[K] = counters.keySet.asJava

  /** for Scala SDK */
  def keys: Set[K] = counters.keySet

  override def hasDelta: Boolean = cleared || removed.nonEmpty || counters.values.exists(_.hasDelta)

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedCounterMap(
      ReplicatedCounterMapDelta(
        cleared = cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        updated = counters.collect {
          case (key, counter) if counter.hasDelta =>
            ReplicatedCounterMapEntryDelta(Some(anySupport.encodeScala(key)), counter.getDelta.counter)
        }.toSeq))

  override def resetDelta(): ReplicatedCounterMapImpl[K] =
    if (hasDelta) new ReplicatedCounterMapImpl(anySupport, counters.view.mapValues(_.resetDelta()).toMap) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedCounterMapImpl[K]] = {
    case ReplicatedEntityDelta.Delta.ReplicatedCounterMap(ReplicatedCounterMapDelta(cleared, removed, updated, _)) =>
      val reducedCounters =
        if (cleared) Map.empty[K, ReplicatedCounterImpl]
        else counters -- removed.map(key => anySupport.decodePossiblyPrimitive(key).asInstanceOf[K])
      val updatedCounters = updated.foldLeft(reducedCounters) {
        case (map, ReplicatedCounterMapEntryDelta(Some(encodedKey), Some(delta), _)) =>
          val key = anySupport.decodePossiblyPrimitive(encodedKey).asInstanceOf[K]
          val counter = map.getOrElse(key, new ReplicatedCounterImpl)
          map.updated(key, counter.applyDelta(ReplicatedEntityDelta.Delta.Counter(delta)))
        case (map, _) => map
      }
      new ReplicatedCounterMapImpl(anySupport, updatedCounters)
  }

  override def toString = s"ReplicatedCounterMap(${counters.map { case (k, v) => s"$k->$v" }.mkString(",")})"

}
