/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import java.util.Objects

import kalix.javasdk.impl.AnySupport
import kalix.javasdk.replicatedentity.ReplicatedRegister
import kalix.protocol.replicated_entity.ReplicatedEntityClock
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.protocol.replicated_entity.ReplicatedRegisterDelta
import com.google.protobuf.any.{ Any => ScalaPbAny }

private[kalix] final class ReplicatedRegisterImpl[T](
    anySupport: AnySupport,
    value: T = null.asInstanceOf[T],
    deltaValue: Option[ScalaPbAny] = None,
    deltaClock: ReplicatedRegister.Clock = ReplicatedRegister.Clock.DEFAULT,
    deltaCustomClockValue: Long = 0)
    extends ReplicatedRegister[T]
    with InternalReplicatedData {

  override type Self = ReplicatedRegisterImpl[T]
  override val name = "ReplicatedRegister"

  override def get(): T = value

  override def set(newValue: T, clock: ReplicatedRegister.Clock, customClockValue: Long): ReplicatedRegisterImpl[T] = {
    Objects.requireNonNull(newValue)
    if (value != newValue || deltaClock != clock || deltaCustomClockValue != customClockValue) {
      new ReplicatedRegisterImpl(anySupport, newValue, Some(anySupport.encodeScala(newValue)), clock, customClockValue)
    } else this
  }

  override def hasDelta: Boolean = deltaValue.isDefined

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Register(
      ReplicatedRegisterDelta(deltaValue, convertClock(deltaClock), deltaCustomClockValue))

  override def resetDelta(): ReplicatedRegisterImpl[T] =
    if (hasDelta) new ReplicatedRegisterImpl(anySupport, value) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedRegisterImpl[T]] = {
    case ReplicatedEntityDelta.Delta.Register(ReplicatedRegisterDelta(Some(any), _, _, _)) =>
      new ReplicatedRegisterImpl(anySupport, anySupport.decodePossiblyPrimitive(any).asInstanceOf[T])
  }

  private def convertClock(clock: ReplicatedRegister.Clock): ReplicatedEntityClock =
    clock match {
      case ReplicatedRegister.Clock.DEFAULT => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_DEFAULT_UNSPECIFIED
      case ReplicatedRegister.Clock.REVERSE => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_REVERSE
      case ReplicatedRegister.Clock.CUSTOM  => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_CUSTOM
      case ReplicatedRegister.Clock.CUSTOM_AUTO_INCREMENT =>
        ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_CUSTOM_AUTO_INCREMENT
    }

  override def toString: String = s"ReplicatedRegister($value)"

}
