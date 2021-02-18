/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.crdt.GSet
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.protocol.crdt.{CrdtDelta, GSetDelta}
import com.google.protobuf.any.{Any => ScalaPbAny}

import java.util
import java.util.Collections
import scala.collection.JavaConverters._

private[crdt] final class GSetImpl[T](anySupport: AnySupport)
    extends util.AbstractSet[T]
    with InternalCrdt
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

  override def delta: CrdtDelta.Delta =
    CrdtDelta.Delta.Gset(GSetDelta(added.asScala.toVector))

  override def resetDelta(): Unit = added.clear()

  override val applyDelta = {
    case CrdtDelta.Delta.Gset(GSetDelta(added, _)) =>
      value.addAll(added.map(e => anySupport.decode(e).asInstanceOf[T]).asJava)
  }

  override def toString = s"GSet(${value.asScala.mkString(",")})"
}
