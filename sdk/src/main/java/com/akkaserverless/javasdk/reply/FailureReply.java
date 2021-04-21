/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.reply;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.Reply;

import java.util.Collection;

/** A failure reply. */
public interface FailureReply<T> extends Reply<T> {

  /**
   * The description of the failure.
   *
   * @return The failure description.
   */
  String description();

  FailureReply<T> addEffects(Collection<Effect> effects);

  FailureReply<T> addEffects(Effect... effects);
}
