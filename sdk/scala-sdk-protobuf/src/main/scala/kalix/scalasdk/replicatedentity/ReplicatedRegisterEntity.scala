/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

abstract class ReplicatedRegisterEntity[T] extends ReplicatedEntity[ReplicatedRegister[T]] {

  /**
   * Implement to set the default empty value for the register.
   *
   * @return
   *   the default empty value
   */
  def emptyValue: T

  /**
   * Implement by returning the initial empty replicated data object. This object will be passed into the command
   * handlers.
   *
   * Also known as the "zero" or "neutral" state.
   *
   * The initial data cannot be `null`.
   *
   * @param factory
   *   the factory to create the initial empty replicated data object
   */
  override final def emptyData(factory: ReplicatedDataFactory): ReplicatedRegister[T] =
    factory.newRegister(emptyValue)
}
