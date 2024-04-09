/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import kalix.scalasdk.impl.ScalaSideEffectAdapter

/* A side effect. */
object SideEffect {

  /**
   * Create a side effect of the given service call.
   *
   * @param deferredCall
   *   The service call to effect.
   * @param synchronous
   *   Whether this effect should be executed synchronously.
   * @return
   *   The side effect.
   */
  def apply[T, R](deferredCall: DeferredCall[T, R], synchronous: Boolean): SideEffect =
    ScalaSideEffectAdapter(deferredCall, synchronous)

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall
   *   The service call to effect.
   * @return
   *   The side effect.
   */
  def apply[T, R](deferredCall: DeferredCall[T, R]): SideEffect =
    ScalaSideEffectAdapter(deferredCall)
}

trait SideEffect {

  /** The service call that is executed as this effect. */
  def serviceCall: DeferredCall[_ <: Any, _ <: Any]

  /** Whether this effect should be executed synchronously or not. */
  def synchronous: Boolean
}
