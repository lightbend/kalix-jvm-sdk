/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.PNCounter
import com.akkaserverless.protocol.replicated_entity.{PNCounterDelta, ReplicatedEntityDelta}

private[replicatedentity] final class PNCounterImpl extends InternalReplicatedData with PNCounter {
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

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Pncounter(PNCounterDelta(deltaValue))

  override def resetDelta(): Unit = deltaValue = 0

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.Pncounter(PNCounterDelta(increment, _)) =>
      value += increment
  }

  override def toString = s"PNCounter($value)"
}
