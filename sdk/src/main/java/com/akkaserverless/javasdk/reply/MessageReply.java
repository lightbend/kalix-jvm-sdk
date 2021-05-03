/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.reply;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.Reply;

import java.util.Collection;

/** A message reply. */
public interface MessageReply<T> extends Reply<T> {

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

  MessageReply<T> addEffects(Collection<Effect> effects);

  MessageReply<T> addEffects(Effect... effects);
}
