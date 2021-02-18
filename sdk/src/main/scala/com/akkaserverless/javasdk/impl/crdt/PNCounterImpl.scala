/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.crdt.PNCounter
import com.akkaserverless.protocol.crdt.{CrdtDelta, PNCounterDelta}

private[crdt] final class PNCounterImpl extends InternalCrdt with PNCounter {
  override final val name = "PNCounter"
  private var value: Long = 0
  private var deltaValue: Long = 0

  override def getValue: Long = value

  override def increment(by: Long): Long = {
    deltaValue += by
    value += by
    value
  }

  override def decrement(by: Long): Long = increment(-by)

  override def hasDelta: Boolean = deltaValue != 0

  override def delta: CrdtDelta.Delta =
    CrdtDelta.Delta.Pncounter(PNCounterDelta(deltaValue))

  override def resetDelta(): Unit = deltaValue = 0

  override val applyDelta = {
    case CrdtDelta.Delta.Pncounter(PNCounterDelta(increment, _)) =>
      value += increment
  }

  override def toString = s"PNCounter($value)"
}
