/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

import com.akkaserverless.javasdk.impl.reply.FailureReplyImpl;
import com.akkaserverless.javasdk.impl.reply.ForwardReplyImpl;
import com.akkaserverless.javasdk.impl.reply.MessageReplyImpl;
import com.akkaserverless.javasdk.impl.reply.NoReply;
import com.akkaserverless.javasdk.reply.FailureReply;
import com.akkaserverless.javasdk.reply.ForwardReply;
import com.akkaserverless.javasdk.reply.MessageReply;

import java.util.Collection;

/**
 * A return type to allow returning forwards or failures, and attaching effects to messages.
 *
 * @param <T> The type of the message that must be returned by this call.
 */
public interface Reply<T> {
  /**
   * Whether this reply is empty: does not have a message, forward, or failure.
   *
   * @return Whether the reply is empty.
   */
  boolean isEmpty();

  /**
   * The effects attached to this reply.
   *
   * @return The effects.
   */
  Collection<Effect> effects();

  /**
   * Attach the given effects to this reply.
   *
   * @param effects The effects to attach.
   * @return A new reply with the attached effects.
   */
  Reply<T> withEffects(Collection<Effect> effects);

  /**
   * Attach the given effects to this reply.
   *
   * @param effects The effects to attach.
   * @return A new reply with the attached effects.
   */
  Reply<T> withEffects(Effect... effects);

  /**
   * Create a message reply.
   *
   * @param payload The payload of the reply.
   * @return A message reply.
   */
  static <T> MessageReply<T> message(T payload) {
    return message(payload, Metadata.EMPTY);
  }

  /**
   * Create a message reply.
   *
   * @param payload The payload of the reply.
   * @param metadata The metadata for the message.
   * @return A message reply.
   */
  static <T> MessageReply<T> message(T payload, Metadata metadata) {
    return new MessageReplyImpl<>(payload, metadata);
  }

  /**
   * Create a forward reply.
   *
   * @param serviceCall The service call representing the forward.
   * @return A forward reply.
   */
  static <T> ForwardReply<T> forward(ServiceCall serviceCall) {
    return new ForwardReplyImpl<>(serviceCall);
  }

  /**
   * Create a failure reply.
   *
   * @param description The description of the failure.
   * @return A failure reply.
   */
  static <T> FailureReply<T> failure(String description) {
    return new FailureReplyImpl<>(description);
  }

  /**
   * Create a reply that contains neither a message nor a forward nor a failure.
   *
   * <p>This may be useful for emitting effects without sending a message.
   *
   * @return The reply.
   */
  static <T> Reply<T> noReply() {
    return NoReply.apply();
  }
}
