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
import com.akkaserverless.protocol.replicated_entity.{
  ReplicatedEntityClock,
  ReplicatedEntityDelta,
  ReplicatedRegisterDelta
}
import com.google.protobuf.any.{Any => ScalaPbAny}
import java.util.Objects

import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegister

private[replicatedentity] final class ReplicatedRegisterImpl[T](anySupport: AnySupport)
    extends InternalReplicatedData
    with ReplicatedRegister[T] {
  override final val name = "ReplicatedRegister"
  private var value: T = _
  private var deltaValue: Option[ScalaPbAny] = None
  private var clock: ReplicatedRegister.Clock = ReplicatedRegister.Clock.DEFAULT
  private var customClockValue: Long = 0

  override def set(value: T, clock: ReplicatedRegister.Clock, customClockValue: Long): T = {
    Objects.requireNonNull(value)
    val old = this.value
    if (this.value != value || this.clock != clock || this.customClockValue != customClockValue) {
      deltaValue = Some(anySupport.encodeScala(value))
      this.value = value
      this.clock = clock
      this.customClockValue = customClockValue
    }
    old
  }

  override def get(): T = value

  override def hasDelta: Boolean = deltaValue.isDefined

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Register(ReplicatedRegisterDelta(deltaValue, convertClock(clock), customClockValue))

  override def resetDelta(): Unit = {
    deltaValue = None
    clock = ReplicatedRegister.Clock.DEFAULT
    customClockValue = 0
  }

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.Register(ReplicatedRegisterDelta(Some(any), _, _, _)) =>
      resetDelta()
      this.value = anySupport.decode(any).asInstanceOf[T]
  }

  private def convertClock(clock: ReplicatedRegister.Clock): ReplicatedEntityClock =
    clock match {
      case ReplicatedRegister.Clock.DEFAULT => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_DEFAULT_UNSPECIFIED
      case ReplicatedRegister.Clock.REVERSE => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_REVERSE
      case ReplicatedRegister.Clock.CUSTOM => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_CUSTOM
      case ReplicatedRegister.Clock.CUSTOM_AUTO_INCREMENT =>
        ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_CUSTOM_AUTO_INCREMENT
    }

  override def toString = s"ReplicatedRegister($value)"
}
