/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.javasdk.valueentity;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.impl.reply.FailureReplyImpl;
import com.akkaserverless.javasdk.impl.reply.ForwardReplyImpl;
import com.akkaserverless.javasdk.impl.reply.MessageReplyImpl;
import com.akkaserverless.javasdk.impl.reply.NoReply;
import com.akkaserverless.javasdk.reply.FailureReply;
import com.akkaserverless.javasdk.reply.ForwardReply;
import com.akkaserverless.javasdk.reply.MessageReply;

/**
 * A return type to allow returning forwards or failures, and attaching effects to messages.
 *
 * @param <T> The type of the message that must be returned by this call.
 */
public abstract class ValueEntityEffect<T> implements Effect<T> {

  public static class Builder<T, S> {
    private Builder() {}

    public Builder2<T, S> updateState(S newState) {
      throw new UnsupportedOperationException("Not implemented yet"); // FIXME
    }

    public Builder2<T, S> deleteState() {
      throw new UnsupportedOperationException("Not implemented yet"); // FIXME
    }

    /**
     * Create a message reply.
     *
     * @param payload The payload of the reply.
     * @return A message reply.
     */
    public MessageReply<T> message(T payload) { // FIXME rename to reply? Note thenReply
      return message(payload, Metadata.EMPTY);
    }

    /**
     * Create a message reply.
     *
     * @param payload The payload of the reply.
     * @param metadata The metadata for the message.
     * @return A message reply.
     */
    public MessageReply<T> message(T payload, Metadata metadata) {
      return new MessageReplyImpl<>(payload, metadata);
    }

    /**
     * Create a forward reply.
     *
     * @param serviceCall The service call representing the forward.
     * @return A forward reply.
     */
    public ForwardReply<T> forward(ServiceCall serviceCall) {
      return new ForwardReplyImpl<>(serviceCall);
    }

    /**
     * Create a failure reply.
     *
     * @param description The description of the failure.
     * @return A failure reply.
     */
    public FailureReply<T> failure(String description) {
      return new FailureReplyImpl<>(description);
    }

    /**
     * Create a reply that contains neither a message nor a forward nor a failure.
     *
     * <p>This may be useful for emitting effects without sending a message.
     *
     * @return The reply.
     */
    public Reply<T> noReply() {
      return NoReply.apply();
    }
  }

  public static class Builder2<T, S> {
    private Builder2() {}

    /**
     * Reply after for example <code>updateState</code>.
     *
     * @param payload The payload of the reply.
     * @return A message reply.
     */
    public MessageReply<T> thenReply(T payload) {
      return thenReply(payload, Metadata.EMPTY);
    }

    /**
     * Reply after for example <code>updateState</code>.
     *
     * @param payload The payload of the reply.
     * @param metadata The metadata for the message.
     * @return A message reply.
     */
    public MessageReply<T> thenReply(T payload, Metadata metadata) {
      throw new UnsupportedOperationException("Not implemented yet"); // FIXME
    }

    // FIXME thenForward
  }

  public static <T, S> Builder<T, S> newBuilder(Class<T> replyClass, Class<S> stateClass) {
    return new Builder<>();
  }
}
