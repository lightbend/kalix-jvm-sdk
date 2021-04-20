/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

/** A context that allows instructing the proxy to perform a side effect. */
public interface EffectContext extends Context {

  /**
   * Invoke the referenced service call as an effect once this action is completed.
   *
   * <p>The effect will be performed asynchronously, ie, the proxy won't wait for the effect to
   * finish before sending the reply.
   *
   * <p>{@link ServiceCall} instances can be created using the {@link ServiceCallFactory} obtained
   * from any (including this) contexts {@link Context#serviceCallFactory()} method.
   *
   * @param effect The service call to make as an effect effect.
   * @deprecated Use return type {@link com.akkaserverless.javasdk.Reply} and attach effects to it instead.
   */
  @Deprecated
  default void effect(ServiceCall effect) {
    this.effect(effect, false);
  }

  /**
   * Invoke the referenced service call as an effect once this action is completed.
   *
   * <p>{@link ServiceCall} instances can be created using the {@link ServiceCallFactory} obtained
   * from any (including this) contexts {@link Context#serviceCallFactory()} method.
   *
   * @param effect The service call to make as an effect effect.
   * @param synchronous Whether the effect should be performed synchronously (ie, wait till it has
   *     finished before sending a reply) or asynchronously.
   * @deprecated Use return type {@link com.akkaserverless.javasdk.Reply} and attach effects to it instead.
   */
  @Deprecated
  void effect(ServiceCall effect, boolean synchronous);
}
