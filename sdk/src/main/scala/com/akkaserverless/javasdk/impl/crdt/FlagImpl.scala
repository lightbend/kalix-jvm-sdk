/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.crdt.Flag
import com.akkaserverless.protocol.crdt.{CrdtDelta, FlagDelta}

private[crdt] final class FlagImpl extends InternalCrdt with Flag {
  override final val name = "Flag"
  private var value: Boolean = false
  private var deltaValue: Boolean = false

  override def isEnabled: Boolean = value

  override def enable(): Unit =
    if (!deltaValue && !value) {
      deltaValue = true
      value = true
    }

  override def hasDelta: Boolean = deltaValue

  override def delta: CrdtDelta.Delta =
    CrdtDelta.Delta.Flag(FlagDelta(deltaValue))

  override def resetDelta(): Unit = deltaValue = false

  override val applyDelta = {
    case CrdtDelta.Delta.Flag(FlagDelta(value, _)) =>
      this.value |= value
  }

  override def toString = s"Flag($value)"
}
