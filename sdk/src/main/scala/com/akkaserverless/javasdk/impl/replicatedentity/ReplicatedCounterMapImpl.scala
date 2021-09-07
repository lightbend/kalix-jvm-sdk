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
import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap
import com.akkaserverless.protocol.replicated_entity.{
  ReplicatedCounterMapDelta,
  ReplicatedCounterMapEntryDelta,
  ReplicatedEntityDelta
}

import java.util.{Set => JSet}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

private[replicatedentity] final class ReplicatedCounterMapImpl[K](
    anySupport: AnySupport,
    _counters: mutable.Map[K, ReplicatedCounterImpl] = mutable.Map.empty[K, ReplicatedCounterImpl]
) extends ReplicatedCounterMap[K]
    with InternalReplicatedData {

  override val name = "ReplicatedCounterMap"

  private val counters = _counters
  private val removed = mutable.Set.empty[K]
  private var cleared = false

  private def getOrCreate(key: K): ReplicatedCounterImpl = counters.getOrElseUpdate(key, new ReplicatedCounterImpl)

  override def get(key: K): Long = counters.get(key).fold(0L)(_.getValue)

  override def increment(key: K, by: Long): Long = getOrCreate(key).increment(by)

  override def decrement(key: K, by: Long): Long = getOrCreate(key).decrement(by)

  override def keySet(): JSet[K] = counters.keySet.asJava

  override def size(): Int = counters.size

  override def isEmpty: Boolean = counters.isEmpty

  override def containsKey(key: K): Boolean = counters.contains(key)

  override def remove(key: K): Unit = if (counters.contains(key)) {
    counters.remove(key)
    removed.add(key)
  }

  override def clear(): Unit = {
    cleared = true
    counters.clear()
    removed.clear()
  }

  override def copy(): ReplicatedCounterMapImpl[K] =
    new ReplicatedCounterMapImpl(anySupport, counters.map {
      case (key, counter) => key -> counter.copy()
    })

  override def hasDelta: Boolean = cleared || removed.nonEmpty || counters.values.exists(_.hasDelta)

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedCounterMap(
      ReplicatedCounterMapDelta(
        cleared = cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        updated = counters.collect {
          case (key, counter) if counter.hasDelta =>
            ReplicatedCounterMapEntryDelta(Some(anySupport.encodeScala(key)), counter.delta.counter)
        }.toSeq
      )
    )

  override def resetDelta(): Unit = {
    counters.values.foreach(_.resetDelta())
    removed.clear()
    cleared = false
  }

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, Unit] = {
    case ReplicatedEntityDelta.Delta.ReplicatedCounterMap(ReplicatedCounterMapDelta(cleared, removed, updated, _)) =>
      if (cleared) counters.clear()
      removed.foreach(key => counters.remove(anySupport.decode(key).asInstanceOf[K]))
      updated.foreach {
        case ReplicatedCounterMapEntryDelta(Some(key), Some(delta), _) =>
          getOrCreate(anySupport.decode(key).asInstanceOf[K]).applyDelta(ReplicatedEntityDelta.Delta.Counter(delta))
        case _ =>
      }
  }
}
