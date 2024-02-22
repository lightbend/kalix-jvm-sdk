/*
 * Copyright 2024 Lightbend Inc.
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
import kalix.javasdk.StatusCode;
import kalix.javasdk.impl.action.ActionContextImpl;
import kalix.javasdk.impl.action.ActionEffectImpl;
import kalix.javasdk.timer.TimerScheduler;
import kalix.javasdk.impl.timer.TimerSchedulerImpl;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Actions are stateless components that can be used to implement different uses cases, such as:
 *
 * <p>
 * <ul>
 *   <li>a pure function.
 *   <li>request conversion - you can use Actions to convert incoming data into a different.
 *   format before forwarding a call to a different component.
 *   <li>as a face or controller to fan out to multiple calls to different components.
 *   <li>publish messages to a Topic.
 *   <li>subscribe to events from an Event Sourced Entity.
 *   <li>subscribe to state changes from a Value Entity.
 *   <li>schedule and cancel Timers.
 * </ul>
 *
 * <p>
 * Actions can be triggered in multiple ways. For example, by:
 *<ul>
 * <li>a gRPC service call.
 * <li>an HTTP service call.
 * <li>a forwarded call from another component.
 * <li>a scheduled call from a Timer.
 * <li>an incoming message from a Topic.
 * <li>an incoming event from an Event Sourced Entity, from within the same service or from a different service. 
 * <li>state changes notification from a Value Entity on the same service.
 *</ul>
 *
 * An Action method should return an {@link Effect} that describes what to do next.
 */
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

  /**
   * INTERNAL API
   */
  public void _internalSetActionContext(Optional<ActionContext> context) {
    actionContext = context;
  }

  public final Effect.Builder effects() {
    return ActionEffectImpl.builder();
  }

  /**
   * Returns a {@link TimerScheduler} that can be used to schedule further in time.
   */
  public final TimerScheduler timers() {
    ActionContextImpl impl =
      (ActionContextImpl)
        actionContext("Timers can only be scheduled or cancelled when handling a message.");
    return new TimerSchedulerImpl(impl.messageCodec(), impl.system(), impl.componentCallMetadata());
  }

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled.
   * You can think of it as a set of instructions you are passing to Kalix. Kalix will process the instructions on your
   * behalf.
   * <p>
   * Each Kalix component defines its own effects, which are a set of predefined
   * operations that match the capabilities of that component.
   * <p>
   * An Action Effect can either:
   * <p>
   * <ul>
   *   <li>reply with a message to the caller
   *   <li>reply with a message to be published to a Topic (in case the method is a publisher)
   *   <li>forward the message to another component
   *   <li>return an error
   *   <li>ignore the call
   * </ul>
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
       * @param <S>     The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <S> Effect<S> reply(S message);

      /**
       * Create a message reply with custom Metadata.
       *
       * @param message  The payload of the reply.
       * @param metadata The metadata for the message.
       * @param <S>      The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <S> Effect<S> reply(S message, Metadata metadata);

      /**
       * Create a forward reply.
       *
       * @param serviceCall The service call representing the forward.
       * @param <S>         The type of the message that must be returned by this call.
       * @return A forward reply.
       */
      <S> Effect<S> forward(DeferredCall<? extends Object, S> serviceCall);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @param <S>         The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <S> Effect<S> error(String description);

      /**
       * Create an error reply with a custom gRPC status code.
       *
       * @param description   The description of the error.
       * @param grpcErrorCode A custom gRPC status code.
       * @param <T>           The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <T> Effect<T> error(String description, Status.Code grpcErrorCode);

      /**
       * Create an error reply with a custom status code.
       * This status code will be translated to an HTTP or gRPC code
       * depending on the type of service being exposed.
       *
       * @param description   The description of the error.
       * @param httpErrorCode A custom Kalix status code to represent the error.
       * @param <T>           The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <T> Effect<T> error(String description, StatusCode.ErrorCode httpErrorCode);

      /**
       * Create a message reply from an async operation result.
       *
       * @param message The future payload of the reply.
       * @param <S>     The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <S> Effect<S> asyncReply(CompletionStage<S> message);

      /**
       * Create a message reply from an async operation result with custom Metadata.
       *
       * @param message The future payload of the reply.
       * @param <S>     The type of the message that must be returned by this call.
       * @param metadata The metadata for the message.
       * @return A message reply.
       */
      <S> Effect<S> asyncReply(CompletionStage<S> message, Metadata metadata);

      /**
       * Create a reply from an async operation result returning an effect.
       *
       * @param futureEffect The future effect to reply with.
       * @param <S>          The type of the message that must be returned by this call.
       * @return A reply, the actual type depends on the nested Effect.
       */
      <S> Effect<S> asyncEffect(CompletionStage<Effect<S>> futureEffect);

      /**
       * Ignore the current element and proceed with processing the next element if returned for an
       * element from a subscription.
       * If used as a response to a regular gRPC or HTTP request it is turned
       * into a NotFound response.
       * <p>
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
