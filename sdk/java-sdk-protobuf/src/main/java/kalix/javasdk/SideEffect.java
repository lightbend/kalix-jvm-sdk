/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import kalix.javasdk.impl.effect.SideEffectImpl;

/** A side effect. */
public interface SideEffect {

  /** The service call that is executed as this effect. */
  DeferredCall<?, ?> call();

  /** Whether this effect should be executed synchronously or not. */
  boolean synchronous();

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall The service call to effect.
   * @param synchronous Whether this effect should be executed synchronously.
   * @return The side effect.
   */
  static SideEffect of(DeferredCall serviceCall, boolean synchronous) {
    return new SideEffectImpl(serviceCall, synchronous);
  }

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall The service call to effect.
   * @return The side effect.
   */
  static SideEffect of(DeferredCall serviceCall) {
    return new SideEffectImpl(serviceCall, false);
  }
}
