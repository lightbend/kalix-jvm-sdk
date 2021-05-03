/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.Flag
import com.akkaserverless.protocol.replicated_entity.{FlagDelta, ReplicatedEntityDelta}

private[replicatedentity] final class FlagImpl extends InternalReplicatedData with Flag {
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

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Flag(FlagDelta(deltaValue))

  override def resetDelta(): Unit = deltaValue = false

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.Flag(FlagDelta(value, _)) =>
      this.value |= value
  }

  override def toString = s"Flag($value)"
}
