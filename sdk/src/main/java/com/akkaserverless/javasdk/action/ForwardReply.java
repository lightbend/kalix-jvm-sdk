/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.ServiceCall;

import java.util.Collection;

/** A forward reply. */
public interface ForwardReply<T> extends ActionReply<T> {

  /**
   * The service call that is being forwarded to.
   *
   * @return The service call.
   */
  ServiceCall serviceCall();

  ForwardReply<T> withEffects(Collection<Effect> effects);

  ForwardReply<T> withEffects(Effect... effects);
}
