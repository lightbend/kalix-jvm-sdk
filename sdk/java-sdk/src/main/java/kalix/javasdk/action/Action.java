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

package kalix.javasdk.action;

import io.grpc.Status;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.SideEffect;
import kalix.javasdk.impl.action.ActionContextImpl;
import kalix.javasdk.impl.action.ActionEffectImpl;
import kalix.javasdk.timer.TimerScheduler;
import kalix.javasdk.impl.timer.TimerSchedulerImpl;
import reactor.core.publisher.Mono;

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
    return actionContext("ActionContext is only available when handling a message.");
  }

  /**
   * INTERNAL API
   *
   * <p>Same as actionContext, but if specific error message when accessing components.
   */
  protected final ActionContext contextForComponents() {
    return actionContext("Components can only be accessed when handling a message.");
  }

  private ActionContext actionContext(String errorMessage) {
    return actionContext.orElseThrow(() -> new IllegalStateException(errorMessage));
  }

  /** INTERNAL API */
  public void _internalSetActionContext(Optional<ActionContext> context) {
    actionContext = context;
  }

  public final Effect.Builder effects() {
    return ActionEffectImpl.builder();
  }

  /** Returns a {@link TimerScheduler} that can be used to schedule further in time. */
  public final TimerScheduler timers() {
    ActionContextImpl impl =
        (ActionContextImpl)
            actionContext("Timers can only be scheduled or cancelled when handling a message.");
    return new TimerSchedulerImpl(impl.messageCodec(), impl.system());
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
      <S> Effect<S> forward(DeferredCall<? extends Object, S> serviceCall);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @return An error reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> error(String description);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @param statusCode A custom gRPC status code.
       * @return An error reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> error(String description, Status.Code statusCode);

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

      /**
       * Create a message reply from an async operation result.
       *
       * @param message The future payload of the reply.
       * @return A message reply.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> asyncReply(Mono<S> message);

      /**
       * Create a reply from an async operation result returning an effect.
       *
       * @param futureEffect The future effect to reply with.
       * @return A reply, the actual type depends on the nested Effect.
       * @param <S> The type of the message that must be returned by this call.
       */
      <S> Effect<S> asyncEffect(Mono<Effect<S>> futureEffect);

      /**
       * Ignore the current element and proceed with processing the next element if returned for an
       * element from eventing in. If used as a response to a regular gRPC or HTTP request it is turned
       * into a NotFound response.
       * 
       * Ignore is not allowed to have side effects added with `addSideEffects`
       */
      <S> Effect<S> ignore();
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

    /**
     * @return true if this effect supports attaching side effects, if returning false addSideEffects will throw an exception.
     */
    boolean canHaveSideEffects();
  }
}
