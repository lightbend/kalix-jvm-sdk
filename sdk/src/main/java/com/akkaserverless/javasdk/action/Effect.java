/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.impl.action.EffectImpl;

/** An effect. */
public interface Effect {

  /** The service call that is executed as this effect. */
  ServiceCall serviceCall();

  /** Whether this effect should be executed synchronously or not. */
  boolean synchronous();

  /**
   * Create an effect of the given service call.
   *
   * @param serviceCall The service call to effect.
   * @param synchronous Whether this effect should be executed synchronously.
   * @return The effect.
   */
  static Effect of(ServiceCall serviceCall, boolean synchronous) {
    return new EffectImpl(serviceCall, synchronous);
  }

  /**
   * Create an effect of the given service call.
   *
   * @param serviceCall The service call to effect.
   * @return The effect.
   */
  static Effect of(ServiceCall serviceCall) {
    return new EffectImpl(serviceCall, false);
  }
}
