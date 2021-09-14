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

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.SideEffect;
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public abstract class Action {

  private volatile Optional<ActionContext> actionContext = Optional.empty();

  /**
   * Additional context and metadata for a message handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected final ActionContext actionContext() {
    return actionContext.orElseThrow(
        () ->
            new IllegalStateException("ActionContext is only available when handling a message."));
  }

  /** INTERNAL API */
  public final void _internalSetActionContext(Optional<ActionContext> context) {
    actionContext = context;
  }

  public final Effect.Builder effects() {
    return ActionEffectImpl.builder();
  }

  /**
   * A return type to allow returning forwards or failures, and attaching effects to messages.
   *
   * @param <T> The type of the message that must be returned by this call.
   */
  public interface Effect<T> {

    /**
     * Construct the effect that is returned by the command handler. The effect describes next
     * processing actions, such as sending a reply.
     */
    interface Builder {
      /**
       * Create a message reply.
       *
       * @param message The payload of the reply.
       * @return A message reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> reply(S message);

      /**
       * Create a message reply.
       *
       * @param message The payload of the reply.
       * @param metadata The metadata for the message.
       * @return A message reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> reply(S message, Metadata metadata);

      /**
       * Create a forward reply.
       *
       * @param serviceCall The service call representing the forward.
       * @return A forward reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> forward(ServiceCall serviceCall);

      /**
       * Create a reply that contains neither a message nor a forward nor an error.
       *
       * @return The reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> noReply();

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @return An error reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> error(String description);

      /**
       * Create a message reply from an async operation result.
       *
       * @param message The future payload of the reply.
       * @return A message reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> asyncReply(CompletionStage<S> message);

      /**
       * Create a reply from an async operation result returning an effect.
       *
       * @param futureEffect The future effect to reply with.
       * @return A reply, the actual type depends on the nested Effect.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> asyncEffect(CompletionStage<Effect<S>> futureEffect);
    }

    /**
     * Attach the given side effects to this reply.
     *
     * @param sideEffects The effects to attach.
     * @return A new reply with the attached effects.
     */
    Effect<T> addSideEffect(SideEffect... sideEffects);

    /**
     * Attach the given side effects to this reply.
     *
     * @param sideEffects The effects to attach.
     * @return A new reply with the attached effects.
     */
    Effect<T> addSideEffects(Collection<SideEffect> sideEffects);
  }
}
