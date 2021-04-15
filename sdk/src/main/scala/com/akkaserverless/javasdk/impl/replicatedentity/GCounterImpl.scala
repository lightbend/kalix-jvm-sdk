/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.GCounter
import com.akkaserverless.protocol.replicated_entity.{GCounterDelta, ReplicatedEntityDelta}

private[replicatedentity] final class GCounterImpl extends InternalReplicatedData with GCounter {
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

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Gcounter(GCounterDelta(deltaValue))

  override def resetDelta(): Unit = deltaValue = 0

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.Gcounter(GCounterDelta(increment, _)) =>
      value += increment
  }

  override def toString = s"GCounter($value)"
}
