/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import kalix.javasdk.impl.replicatedentity.ReplicatedRegisterImpl
import kalix.javasdk.replicatedentity.{ ReplicatedRegister => JavaSdkReplicatedRegister }
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

object ReplicatedRegister {
  sealed trait Clock
  case object Default extends Clock
  case object Reverse extends Clock
  case object Custom extends Clock
  case object CustomAutoIncrement extends Clock
}

/**
 * A Last-Write-Wins Register.
 *
 * This uses a clock value to determine which of two concurrent writes should win. When both clock values are the same,
 * an ordering defined over the node addresses is used to break the tie.
 *
 * By default, the clock used is the clock of the node that set the value. This can be affected by clock skew, which
 * means two successive writes delegated to two separate nodes could result in the first one winning. This can be
 * avoided by using a custom clock with a domain specific clock value, if such a causally ordered value is available.
 *
 * @tparam T
 */
class ReplicatedRegister[T] private[scalasdk] (override val delegate: ReplicatedRegisterImpl[T])
    extends InternalReplicatedData {

  /**
   * Optionally returns the current value of the register.
   *
   * @return
   *   an option value containing the current value of the register if initialize, or `None`.
   */
  def get: Option[T] = Option(delegate.get())

  /**
   * Get the current value of the register.
   *
   * @param key
   *   the key of the mapping
   * @return
   *   the current value of the register
   * @throws java.util.NoSuchElementException
   *   if value not defined
   */
  def apply(): T = get.getOrElse(throw new NoSuchElementException("Register value not defined"))

  /**
   * Set the value of the register, using the default clock.
   *
   * @param value
   *   the new value of the register
   * @return
   *   a new register with updated value
   */
  def set(value: T): ReplicatedRegister[T] =
    new ReplicatedRegister(delegate.set(value, JavaSdkReplicatedRegister.Clock.DEFAULT, 0))

  /**
   * Set the current value of the register, using the given custom clock and clock value if required.
   *
   * @param value
   *   the new value of the register
   * @param clock
   *   the clock to use for replication
   * @param customClockValue
   *   the custom clock value to use, only if it's a custom clock
   * @return
   *   a new register with updated value
   */
  def set(value: T, clock: ReplicatedRegister.Clock, customClockValue: Long): ReplicatedRegister[T] = {
    val javaClock =
      clock match {
        case ReplicatedRegister.Default             => JavaSdkReplicatedRegister.Clock.DEFAULT
        case ReplicatedRegister.Reverse             => JavaSdkReplicatedRegister.Clock.REVERSE
        case ReplicatedRegister.Custom              => JavaSdkReplicatedRegister.Clock.CUSTOM
        case ReplicatedRegister.CustomAutoIncrement => JavaSdkReplicatedRegister.Clock.CUSTOM_AUTO_INCREMENT
      }
    new ReplicatedRegister(delegate.set(value, javaClock, customClockValue))
  }

  final override type Self = ReplicatedRegister[T]

  final override def resetDelta(): ReplicatedRegister[T] =
    new ReplicatedRegister(delegate.resetDelta())

  final override def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedRegister[T]] =
    delegate.applyDelta.andThen(new ReplicatedRegister(_))
}
