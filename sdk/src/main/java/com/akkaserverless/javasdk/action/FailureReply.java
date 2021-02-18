/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import java.util.Collection;

/** A failure reply. */
public interface FailureReply<T> extends ActionReply<T> {

  /**
   * The description of the failure.
   *
   * @return The failure description.
   */
  String description();

  FailureReply<T> withEffects(Collection<Effect> effects);

  FailureReply<T> withEffects(Effect... effects);
}
