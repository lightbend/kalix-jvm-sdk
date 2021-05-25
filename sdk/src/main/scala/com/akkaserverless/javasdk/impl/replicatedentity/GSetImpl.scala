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

import com.akkaserverless.javasdk.replicatedentity.GSet
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.protocol.replicated_entity.{GSetDelta, ReplicatedEntityDelta}
import com.google.protobuf.any.{Any => ScalaPbAny}

import java.util
import java.util.Collections
import scala.jdk.CollectionConverters._

private[replicatedentity] final class GSetImpl[T](anySupport: AnySupport)
    extends util.AbstractSet[T]
    with InternalReplicatedData
    with GSet[T] {
  override final val name = "GSet"
  private val value = new util.HashSet[T]()
  private val added = new util.HashSet[ScalaPbAny]()

  override def size(): Int = value.size()

  override def isEmpty: Boolean = super.isEmpty

  override def contains(o: Any): Boolean = value.contains(o)

  override def add(e: T): Boolean =
    if (value.contains(e)) {
      false
    } else {
      added.add(anySupport.encodeScala(e))
      value.add(e)
    }

  override def remove(o: Any): Boolean = throw new UnsupportedOperationException("Cannot remove elements from a GSet")

  override def iterator(): util.Iterator[T] = Collections.unmodifiableSet(value).iterator()

  override def hasDelta: Boolean = !added.isEmpty

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Gset(GSetDelta(added.asScala.toVector))

  override def resetDelta(): Unit = added.clear()

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.Gset(GSetDelta(added, _)) =>
      value.addAll(added.map(e => anySupport.decode(e).asInstanceOf[T]).asJava)
  }

  override def toString = s"GSet(${value.asScala.mkString(",")})"
}
