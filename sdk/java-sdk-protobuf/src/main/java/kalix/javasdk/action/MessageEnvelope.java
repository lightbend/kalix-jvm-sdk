/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.action;

import kalix.javasdk.Metadata;
import kalix.javasdk.impl.action.MessageEnvelopeImpl;

/** A message envelope. */
public interface MessageEnvelope<T> {
  /**
   * The metadata associated with the message.
   *
   * @return The metadata.
   */
  Metadata metadata();

  /**
   * The payload of the message.
   *
   * @return The payload.
   */
  T payload();

  /**
   * Create a message.
   *
   * @param payload The payload of the message.
   * @return The message.
   */
  static <T> MessageEnvelope<T> of(T payload) {
    return new MessageEnvelopeImpl<>(payload, Metadata.EMPTY);
  }

  /**
   * Create a message.
   *
   * @param payload The payload of the message.
   * @param metadata The metadata associated with the message.
   * @return The message.
   */
  static <T> MessageEnvelope<T> of(T payload, Metadata metadata) {
    return new MessageEnvelopeImpl<>(payload, metadata);
  }
}
