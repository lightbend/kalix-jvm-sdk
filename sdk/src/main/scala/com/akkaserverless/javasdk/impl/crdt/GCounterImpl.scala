/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.crdt.GCounter
import com.akkaserverless.protocol.crdt.{CrdtDelta, GCounterDelta}

private[crdt] final class GCounterImpl extends InternalCrdt with GCounter {
  override final val name = "GCounter"
  private var value: Long = 0
  private var deltaValue: Long = 0

  override def getValue: Long = value

  override def increment(by: Long): Long = {
    if (by < 0) {
      throw new IllegalArgumentException("Cannot increment a GCounter by a negative amount.")
    }
    deltaValue += by
    value += by
    value
  }

  override def hasDelta: Boolean = deltaValue != 0

  override def delta: CrdtDelta.Delta =
    CrdtDelta.Delta.Gcounter(GCounterDelta(deltaValue))

  override def resetDelta(): Unit = deltaValue = 0

  override val applyDelta = {
    case CrdtDelta.Delta.Gcounter(GCounterDelta(increment, _)) =>
      value += increment
  }

  override def toString = s"GCounter($value)"
}
