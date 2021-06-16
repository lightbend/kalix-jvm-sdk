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
import com.google.protobuf.any.{Any => ScalaPbAny}
import org.slf4j.LoggerFactory
import java.util
import java.util.function

import scala.jdk.CollectionConverters._

private object ReplicatedMapImpl {
  private val log = LoggerFactory.getLogger(classOf[ReplicatedMapImpl[_, _]])
}

/**
 * A few notes on implementation:
 *
 * - put, and any similar operations (such as Map.Entry.setValue) are not supported, because the only way to create a
 *   Replicated Data object is using a ReplicatedDataFactory, and we only make ReplicatedDataFactory available in very
 *   specific contexts, such as in the getOrCreate method. The getOrCreate method is the only way to insert something
 *   new into the map.
 * - All mechanisms for removal are supported - eg, calling remove directly, calling remove on any of the derived sets
 *   (entrySet, keySet, values), and calling remove on the entrySet iterator.
 * - ju.AbstractMap is very useful, though bases most of its implementation on entrySet, so we need to take care to
 *   efficiently implement operations that it implements in O(n) time that we can do in O(1) time, such as
 *   get/remove/containsKey.
 */
private[replicatedentity] final class ReplicatedMapImpl[K, V <: InternalReplicatedData](anySupport: AnySupport)
    extends ReplicatedMap[K, V]
    with InternalReplicatedData {
  import ReplicatedMapImpl.log

  override final val name = "ReplicatedMap"
  private val value = new util.HashMap[K, V]()
  private val added = new util.HashMap[K, (ScalaPbAny, V)]()
  private val removed = new util.HashSet[ScalaPbAny]()
  private var cleared = false

  override def getOrCreate(key: K, create: function.Function[ReplicatedDataFactory, V]): V =
    if (value.containsKey(key)) {
      value.get(key)
    } else {
      val encodedKey = anySupport.encodeScala(key)
      var internalData: InternalReplicatedData = null
      val data = create(new AbstractReplicatedEntityFactory {
        override protected def anySupport: AnySupport = ReplicatedMapImpl.this.anySupport
        override protected def newEntity[C <: InternalReplicatedData](entity: C): C = {
          if (internalData != null) {
            throw new IllegalStateException(
              "getOrCreate creation callback must only be used to create one replicated data item at a time"
            )
          }
          internalData = entity
          entity
        }
      })
      if (data == null) {
        throw new IllegalArgumentException("getOrCreate creation callback must return a Replicated Data object")
      } else if (data != internalData) {
        throw new IllegalArgumentException(
          "Replicated Data returned by getOrCreate creation callback must have been created by the ReplicatedDataFactory passed to it"
        )
      }

      value.put(key, data)
      added.put(key, (encodedKey, data))
      data
    }

  override def containsKey(key: K): Boolean = value.containsKey(key)

  override def get(key: K): V = value.get(key)

  override def remove(key: K): Unit = {
    if (value.containsKey(key)) {
      val encodedKey = anySupport.encodeScala(key)
      if (added.containsKey(key)) {
        added.remove(key)
      } else {
        removed.add(encodedKey)
      }
    }
    value.remove(key)
  }

  override def keySet(): util.Set[K] = value.keySet()

  override def size(): Int = value.size()

  override def isEmpty: Boolean = value.isEmpty

  override def clear(): Unit = {
    value.clear()
    cleared = true
    removed.clear()
    added.clear()
  }

  override def hasDelta: Boolean =
    if (cleared || !added.isEmpty || !removed.isEmpty) {
      true
    } else {
      value.values().asScala.exists(_.hasDelta)
    }

  override def delta: ReplicatedEntityDelta.Delta = {
    val updated = (value.asScala -- this.added.keySet().asScala).collect {
      case (key, changed) if changed.hasDelta =>
        ReplicatedMapEntryDelta(Some(anySupport.encodeScala(key)), Some(ReplicatedEntityDelta(changed.delta)))
    }
    val added = this.added.asScala.values.map {
      case (key, value) => ReplicatedMapEntryDelta(Some(key), Some(ReplicatedEntityDelta(value.delta)))
    }

    ReplicatedEntityDelta.Delta.ReplicatedMap(
      ReplicatedMapDelta(
        cleared = cleared,
        removed = removed.asScala.toVector,
        updated = updated.toVector,
        added = added.toVector
      )
    )
  }

  override def resetDelta(): Unit = {
    cleared = false
    added.clear()
    removed.clear()
    value.values().asScala.foreach(_.resetDelta())
  }

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.ReplicatedMap(ReplicatedMapDelta(cleared, removed, updated, added, _)) =>
      if (cleared) {
        value.clear()
      }
      removed.foreach(key => value.remove(anySupport.decode(key)))
      updated.foreach {
        case ReplicatedMapEntryDelta(Some(key), Some(delta), _) =>
          val data = value.get(anySupport.decode(key))
          if (data == null) {
            log.warn("ReplicatedMap entry to update with key [{}] not found in map", key)
          } else {
            data.applyDelta(delta.delta)
          }
      }
      added.foreach {
        case ReplicatedMapEntryDelta(Some(key), Some(delta), _) =>
          value.put(anySupport.decode(key).asInstanceOf[K],
                    ReplicatedEntityDeltaTransformer.create(delta, anySupport).asInstanceOf[V])
      }
  }

  override def toString = s"ReplicatedMap(${value.asScala.map { case (k, v) => s"$k->$v" }.mkString(",")})"
}
