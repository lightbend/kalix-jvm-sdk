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
import com.akkaserverless.javasdk.replicatedentity.{ReplicatedRegister, ReplicatedRegisterMap}
import com.akkaserverless.protocol.replicated_entity.{
  ReplicatedEntityDelta,
  ReplicatedRegisterMapDelta,
  ReplicatedRegisterMapEntryDelta
}

import java.util.{Optional, Set => JSet}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

private[replicatedentity] final class ReplicatedRegisterMapImpl[K, V](anySupport: AnySupport)
    extends ReplicatedRegisterMap[K, V]
    with InternalReplicatedData {

  override val name = "ReplicatedRegisterMap"

  private val registers = mutable.Map.empty[K, ReplicatedRegisterImpl[V]]
  private val removed = mutable.Set.empty[K]
  private var cleared = false

  private def getOrCreate(key: K): ReplicatedRegisterImpl[V] =
    registers.getOrElseUpdate(key, new ReplicatedRegisterImpl[V](anySupport))

  override def getValue(key: K): Optional[V] = registers.get(key).map(_.get()).toJava

  override def setValue(key: K, value: V, clock: ReplicatedRegister.Clock, customClockValue: Long): Unit =
    getOrCreate(key).set(value, clock, customClockValue)

  override def keySet(): JSet[K] = registers.keySet.asJava

  override def size(): Int = registers.size

  override def isEmpty: Boolean = registers.isEmpty

  override def containsKey(key: K): Boolean = registers.contains(key)

  override def remove(key: K): Unit = if (registers.contains(key)) {
    registers.remove(key)
    removed.add(key)
  }

  override def clear(): Unit = {
    cleared = true
    registers.clear()
    removed.clear()
  }

  override def hasDelta: Boolean = cleared || removed.nonEmpty || registers.values.exists(_.hasDelta)

  override def delta: ReplicatedEntityDelta.Delta = ReplicatedEntityDelta.Delta.ReplicatedRegisterMap(
    ReplicatedRegisterMapDelta(
      cleared = cleared,
      removed = removed.map(anySupport.encodeScala).toSeq,
      updated = registers.collect {
        case (key, register) if register.hasDelta =>
          ReplicatedRegisterMapEntryDelta(Some(anySupport.encodeScala(key)), register.delta.register)
      }.toSeq
    )
  )

  override def resetDelta(): Unit = {
    registers.values.foreach(_.resetDelta())
    removed.clear()
    cleared = false
  }

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, Unit] = {
    case ReplicatedEntityDelta.Delta.ReplicatedRegisterMap(ReplicatedRegisterMapDelta(cleared, removed, updated, _)) =>
      if (cleared) registers.clear()
      removed.foreach(key => registers.remove(anySupport.decode(key).asInstanceOf[K]))
      updated.foreach {
        case ReplicatedRegisterMapEntryDelta(Some(key), Some(delta), _) =>
          getOrCreate(anySupport.decode(key).asInstanceOf[K]).applyDelta(ReplicatedEntityDelta.Delta.Register(delta))
        case _ =>
      }
  }
}
