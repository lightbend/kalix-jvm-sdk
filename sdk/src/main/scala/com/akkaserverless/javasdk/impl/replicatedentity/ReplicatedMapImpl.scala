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

import com.akkaserverless.javasdk.replicatedentity.{ReplicatedDataFactory, ReplicatedMap}
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.protocol.replicated_entity.{
  ReplicatedEntityDelta,
  ReplicatedMapDelta,
  ReplicatedMapEntryDelta
}
import org.slf4j.LoggerFactory

import java.util.function
import scala.jdk.CollectionConverters._

private object ReplicatedMapImpl {
  private val log = LoggerFactory.getLogger(classOf[ReplicatedMapImpl[_, _]])
}

private[replicatedentity] final class ReplicatedMapImpl[K, V <: InternalReplicatedData](
    anySupport: AnySupport,
    entries: Map[K, V] = Map.empty[K, V],
    added: Set[K] = Set.empty[K],
    removed: Set[K] = Set.empty[K],
    cleared: Boolean = false
) extends ReplicatedMap[K, V]
    with InternalReplicatedData {

  import ReplicatedMapImpl.log

  override type Self = ReplicatedMapImpl[K, V]
  override val name = "ReplicatedMap"

  override def get(key: K): V = entries(key)

  override def getOrElse(key: K, create: function.Function[ReplicatedDataFactory, V]): V =
    entries.getOrElse(
      key, {
        val dataFactory = new ReplicatedDataFactoryImpl(anySupport)
        val data = create(dataFactory)
        if (data eq null) {
          throw new IllegalArgumentException(
            "Replicated Map getOrElse creation function must return a Replicated Data object"
          )
        } else if (data ne dataFactory.internalData) {
          throw new IllegalArgumentException(
            "Replicated Data returned by getOrElse creation callback must have been created by the ReplicatedDataFactory passed to it"
          )
        }
        data
      }
    )

  override def update(key: K, value: V): ReplicatedMap[K, V] =
    new ReplicatedMapImpl(
      anySupport,
      entries.updated(key, value),
      if (entries.contains(key)) added else added + key,
      removed,
      cleared
    )

  override def remove(key: K): ReplicatedMapImpl[K, V] = {
    if (!entries.contains(key)) {
      this
    } else {
      if (entries.size == 1) { // just the to-be-removed mapping
        clear()
      } else {
        if (added.contains(key)) {
          new ReplicatedMapImpl(anySupport, entries - key, added - key, removed, cleared)
        } else {
          new ReplicatedMapImpl(anySupport, entries - key, added, removed + key, cleared)
        }
      }
    }
  }

  override def clear(): ReplicatedMapImpl[K, V] =
    new ReplicatedMapImpl[K, V](anySupport, cleared = true)

  override def size: Int = entries.size

  override def isEmpty: Boolean = entries.isEmpty

  override def containsKey(key: K): Boolean = entries.contains(key)

  override def keySet: java.util.Set[K] = entries.keySet.asJava

  override def hasDelta: Boolean =
    if (cleared || added.nonEmpty || removed.nonEmpty) {
      true
    } else {
      entries.values.exists(_.hasDelta)
    }

  override def getDelta: ReplicatedEntityDelta.Delta = {
    val updatedEntries = (entries -- added).collect {
      case (key, changed) if changed.hasDelta =>
        ReplicatedMapEntryDelta(Some(anySupport.encodeScala(key)), Some(ReplicatedEntityDelta(changed.getDelta)))
    }
    val addedEntries = added.flatMap { key =>
      entries.get(key).map { value =>
        ReplicatedMapEntryDelta(Some(anySupport.encodeScala(key)), Some(ReplicatedEntityDelta(value.getDelta)))
      }
    }
    ReplicatedEntityDelta.Delta.ReplicatedMap(
      ReplicatedMapDelta(
        cleared = cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        updated = updatedEntries.toSeq,
        added = addedEntries.toSeq
      )
    )
  }

  override def resetDelta(): ReplicatedMapImpl[K, V] =
    if (!hasDelta) this
    else new ReplicatedMapImpl(anySupport, entries.view.mapValues(_.resetDelta().asInstanceOf[V]).toMap)

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedMapImpl[K, V]] = {
    case ReplicatedEntityDelta.Delta.ReplicatedMap(ReplicatedMapDelta(cleared, removed, updated, added, _)) =>
      val reducedEntries =
        if (cleared) Map.empty[K, V]
        else entries -- removed.map(key => anySupport.decode(key).asInstanceOf[K])
      val updatedEntries = updated.foldLeft(reducedEntries) {
        case (map, ReplicatedMapEntryDelta(Some(encodedKey), Some(ReplicatedEntityDelta(delta, _)), _)) =>
          val key = anySupport.decode(encodedKey).asInstanceOf[K]
          map.get(key) match {
            case Some(value) => map.updated(key, value.applyDelta(delta).asInstanceOf[V])
            case _ => log.warn("ReplicatedMap entry to update with key [{}] not found in map", key); map
          }
        case (map, _) => map
      }
      val newEntries = added.foldLeft(updatedEntries) {
        case (map, ReplicatedMapEntryDelta(Some(encodedKey), Some(delta), _)) =>
          val key = anySupport.decode(encodedKey).asInstanceOf[K]
          map.updated(key, ReplicatedEntityDeltaTransformer.create(delta, anySupport).asInstanceOf[V])
        case (map, _) => map
      }
      new ReplicatedMapImpl(anySupport, newEntries)
  }

  override def toString = s"ReplicatedMap(${entries.map { case (k, v) => s"$k->$v" }.mkString(",")})"
}
