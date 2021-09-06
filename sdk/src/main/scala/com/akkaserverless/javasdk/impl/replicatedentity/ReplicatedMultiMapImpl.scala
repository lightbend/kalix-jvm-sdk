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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap
import com.akkaserverless.protocol.replicated_entity.{
  ReplicatedEntityDelta,
  ReplicatedMultiMapDelta,
  ReplicatedMultiMapEntryDelta
}

import java.util.{Collection => JCollection, Collections => JCollections, Set => JSet}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

private[replicatedentity] final class ReplicatedMultiMapImpl[K, V](
    anySupport: AnySupport,
    _entries: mutable.Map[K, ReplicatedSetImpl[V]] = mutable.Map.empty[K, ReplicatedSetImpl[V]]
) extends ReplicatedMultiMap[K, V]
    with InternalReplicatedData {

  override val name = "ReplicatedMultiMap"

  private val entries = _entries
  private val removed = mutable.Set.empty[K]
  private var cleared = false

  private def getOrCreate(key: K): ReplicatedSetImpl[V] =
    entries.getOrElseUpdate(key, new ReplicatedSetImpl[V](anySupport))

  override def get(key: K): JSet[V] = entries.get(key).fold(JCollections.emptySet[V])(_.elements)

  override def put(key: K, value: V): Boolean =
    getOrCreate(key).add(value)

  override def putAll(key: K, values: JCollection[V]): Boolean =
    if (values.isEmpty) false else getOrCreate(key).addAll(values)

  override def remove(key: K, value: V): Boolean = {
    entries.get(key).fold(false) { values =>
      val isRemoved = values.remove(value)
      if (values.isEmpty) removeAll(key) else isRemoved
    }
  }

  override def removeAll(key: K): Boolean = {
    val isRemoved = entries.remove(key).isDefined
    if (isRemoved) removed.add(key)
    isRemoved
  }

  override def clear(): Unit = {
    cleared = true
    entries.clear()
    removed.clear()
  }

  override def keySet(): JSet[K] = entries.keySet.asJava

  override def size(): Int = entries.values.map(_.size()).sum

  override def isEmpty: Boolean = entries.isEmpty

  override def containsKey(key: K): Boolean = entries.contains(key)

  override def containsValue(key: K, value: V): Boolean = entries.get(key).fold(false)(_.contains(value))

  override def copy(): ReplicatedMultiMapImpl[K, V] =
    new ReplicatedMultiMapImpl(anySupport, entries.map {
      case (key, set) => key -> set.copy()
    })

  override def hasDelta: Boolean = cleared || removed.nonEmpty || entries.values.exists(_.hasDelta)

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedMultiMap(
      ReplicatedMultiMapDelta(
        cleared = cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        updated = entries.collect {
          case (key, values) if values.hasDelta =>
            ReplicatedMultiMapEntryDelta(Some(anySupport.encodeScala(key)), values.delta.replicatedSet)
        }.toSeq
      )
    )

  override def resetDelta(): Unit = {
    entries.values.foreach(_.resetDelta())
    removed.clear()
    cleared = false
  }

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, Unit] = {
    case ReplicatedEntityDelta.Delta.ReplicatedMultiMap(ReplicatedMultiMapDelta(cleared, removed, updated, _)) =>
      if (cleared) entries.clear()
      removed.foreach(key => entries.remove(anySupport.decode(key).asInstanceOf[K]))
      updated.foreach {
        case ReplicatedMultiMapEntryDelta(Some(key), Some(delta), _) =>
          getOrCreate(anySupport.decode(key).asInstanceOf[K])
            .applyDelta(ReplicatedEntityDelta.Delta.ReplicatedSet(delta))
        case _ =>
      }
  }
}
