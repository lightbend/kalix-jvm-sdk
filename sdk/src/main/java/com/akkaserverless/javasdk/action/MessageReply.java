/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.Metadata;

import java.util.Collection;

/** A message reply. */
public interface MessageReply<T> extends ActionReply<T> {

  /**
   * The payload of the message reply.
   *
   * @return The payload.
   */
  T payload();

  /**
   * The metadata associated with the message.
   *
   * @return The metadata.
   */
  Metadata metadata();

  MessageReply<T> withEffects(Collection<Effect> effects);

  MessageReply<T> withEffects(Effect... effects);
}
