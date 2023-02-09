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

package kalix.javasdk.eventsourcedentity;

import akka.annotation.ApiMayChange;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
import kalix.javasdk.Metadata;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.SideEffect;
import io.grpc.Status;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** @param <S> The type of the state for this entity. */
public abstract class EventSourcedEntity<S> {

  private Optional<CommandContext> commandContext = Optional.empty();
  private Optional<EventContext> eventContext = Optional.empty();
  private Optional<S> currentState = Optional.empty();
  private boolean handlingCommands = false;

  /**
   * Implement by returning the initial empty state object. This object will be passed into the
   * command and event handlers, until a new state replaces it.
   *
   * <p>Also known as "zero state" or "neutral state".
   *
   * <p>The default implementation of this method returns <code>null</code>. It can be overridden to
   * return a more sensible initial state.
   */
  public S emptyState() {
    return null;
  }

  /**
   * Additional context and metadata for a command handler.
   *
   * <p>It will throw an exception if accessed from constructor or event handler.
   */
  protected final CommandContext commandContext() {
    return commandContext.orElseThrow(
        () ->
            new IllegalStateException("CommandContext is only available when handling a command."));
  }

  /** INTERNAL API */
  public void _internalSetCommandContext(Optional<CommandContext> context) {
    commandContext = context;
  }

  /**
   * Additional context and metadata for an event handler.
   *
   * <p>It will throw an exception if accessed from constructor or command handler.
   */
  protected final EventContext eventContext() {
    return eventContext.orElseThrow(
        () -> new IllegalStateException("EventContext is only available when handling an event."));
  }

  /** INTERNAL API */
  public void _internalSetEventContext(Optional<EventContext> context) {
    eventContext = context;
  }

  /** INTERNAL API */
  public void _internalSetCurrentState(S state) {
    handlingCommands = true;
    currentState = Optional.ofNullable(state);
  }

  /**
   * Returns the state as currently stored by Kalix.
   *
   * <p>Note that modifying the state directly will not update it in storage. To save the state, one
   * must call {{@code effects().updateState()}}.
   *
   * <p>This method can only be called when handling a command or an event. Calling it outside a
   * method (eg: in the constructor) will raise a IllegalStateException exception.
   *
   * @throws IllegalStateException if accessed outside a handler method
   */
  @ApiMayChange
  protected final S currentState() {
    // user may call this method inside a command handler and get a null because it's legal
    // to have emptyState set to null.
    if (handlingCommands) return currentState.orElse(null);
    else
      throw new IllegalStateException("Current state is only available when handling a command.");
  }

  protected final Effect.Builder<S> effects() {
    return new EventSourcedEntityEffectImpl<S>();
  }

  /**
   * A return type to allow returning forwards or failures, and attaching effects to messages.
   *
   * @param <T> The type of the message that must be returned by this call.
   */
  public interface Effect<T> {

    /**
     * Construct the effect that is returned by the command handler. The effect describes next
     * processing actions, such as emitting events and sending a reply.
     *
     * @param <S> The type of the state for this entity.
     */
    interface Builder<S> {

      OnSuccessBuilder<S> emitEvent(Object event);

      OnSuccessBuilder<S> emitEvents(List<?> event);

      /**
       * Create a message reply.
       *
       * @param message The payload of the reply.
       * @return A message reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> reply(T message);

      /**
       * Create a message reply.
       *
       * @param message The payload of the reply.
       * @param metadata The metadata for the message.
       * @return A message reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> reply(T message, Metadata metadata);

      /**
       * Create a forward reply.
       *
       * @param serviceCall The service call representing the forward.
       * @return A forward reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> forward(DeferredCall<? extends Object, T> serviceCall);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @return An error reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> error(String description);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @param statusCode A custom gRPC status code.
       * @return An error reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> error(String description, Status.Code statusCode);
    }

    interface OnSuccessBuilder<S> {

      /**
       * Delete the entity. No addition events are allowed.
       */
      OnSuccessBuilder<S> deleteEntity();

      /**
       * Reply after for example <code>emitEvent</code>.
       *
       * @param replyMessage Function to create the reply message from the new state.
       * @return A message reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> thenReply(Function<S, T> replyMessage);

      /**
       * Reply after for example <code>emitEvent</code>.
       *
       * @param replyMessage Function to create the reply message from the new state.
       * @param metadata The metadata for the message.
       * @return A message reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> thenReply(Function<S, T> replyMessage, Metadata metadata);

      /**
       * Create a forward reply after for example <code>emitEvent</code>.
       *
       * @param serviceCall The service call representing the forward.
       * @return A forward reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> thenForward(Function<S, DeferredCall<? extends Object, T>> serviceCall);

      /**
       * Attach the given side effect to this reply from the new state.
       *
       * @param sideEffect The effect to attach.
       * @return A new reply with the attached effect.
       */
      OnSuccessBuilder<S> thenAddSideEffect(Function<S, SideEffect> sideEffect);
    }

    /**
     * Attach the given side effects to this reply.
     *
     * @param sideEffects The effects to attach.
     * @return A new reply with the attached effects.
     */
    Effect<T> addSideEffects(Collection<SideEffect> sideEffects);

    /**
     * Attach the given effects to this reply.
     *
     * @param effects The effects to attach.
     * @return A new reply with the attached effects.
     */
    Effect<T> addSideEffects(SideEffect... effects);
  }
}
