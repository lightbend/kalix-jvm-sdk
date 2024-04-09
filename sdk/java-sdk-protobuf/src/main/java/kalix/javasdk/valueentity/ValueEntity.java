/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.valueentity;

import akka.annotation.ApiMayChange;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.SideEffect;
import kalix.javasdk.StatusCode;
import kalix.javasdk.action.Action;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.impl.valueentity.ValueEntityEffectImpl;
import io.grpc.Status;

import java.util.Collection;
import java.util.Optional;

/**
 * Value Entities persist their state on every change. You can think of them as a Key-Value entity where
 * the key is the entity id and the value is the state of the entity.
 * <p>
 * Kalix Value Entities have nothing in common with the domain-driven design concept of Value Objects.
 * The Value in the name refers to the direct modification of the entity's state.
 *
 * When implementing a Value Entity, you first define what will be its internal state (your domain model),
 * and the commands it will handle (mutation requests).
 * <p>
 * Each command is handled by a command handler. Command handlers are methods returning an {@link Effect}.
 * When handling a command, you use the Effect API to:
 * <p>
 * <ul>
 *   <li>update the entity state and send a reply to the caller
 *   <li>directly reply to the caller if the command is not requesting any state change
 *   <li>rejected the command by returning an error
 *   <li>instruct Kalix to delete the entity
 * </ul>
 *
 * @param <S> The type of the state for this entity. */
public abstract class ValueEntity<S> {

  private Optional<CommandContext> commandContext = Optional.empty();

  private Optional<S> currentState = Optional.empty();

  private boolean handlingCommands = false;

  /**
   * Implement by returning the initial empty state object. This object will be passed into the
   * command handlers, until a new state replaces it.
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
   * <p>It will throw an exception if accessed from constructor.
   *
   * @throws IllegalStateException if accessed outside a handler method
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
   * <p>This method can only be called when handling a command. Calling it outside a method (eg: in
   * the constructor) will raise a IllegalStateException exception.
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
    return new ValueEntityEffectImpl<S>();
  }

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled.
   * You can think of it as a set of instructions you are passing to Kalix. Kalix will process the instructions on your
   * behalf and ensure that any data that needs to be persisted will be persisted.
   * <p>
   * Each Kalix component defines its own effects, which are a set of predefined
   * operations that match the capabilities of that component.
   * <p>
   * A ValueEntity Effect can either:
   * <p>
   * <ul>
   *   <li>update the entity state and send a reply to the caller
   *   <li>directly reply to the caller if the command is not requesting any state change
   *   <li>rejected the command by returning an error
   *   <li>instruct Kalix to delete the entity
   * </ul>
   *
   * @param <T> The type of the message that must be returned by this call.
   */
  public interface Effect<T> {

    /**
     * Construct the effect that is returned by the command handler. The effect describes next
     * processing actions, such as updating state and sending a reply.
     *
     * @param <S> The type of the state for this entity.
     */
    interface Builder<S> {

      OnSuccessBuilder<S> updateState(S newState);

      /**
       * Delete the entity. No additional updates are allowed afterwards.
       */
      OnSuccessBuilder<S> deleteEntity();

      /**
       * Delete the entity. No additional updates are allowed afterwards.
       *
       * @deprecated Renamed to deleteEntity
       */
      @Deprecated(since = "1.1.5", forRemoval = true)
      OnSuccessBuilder<S> deleteState();

      /**
       * Create a message reply.
       *
       * @param message The payload of the reply.
       * @param <T> The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <T> Effect<T> reply(T message);

      /**
       * Create a message reply.
       *
       * @param message The payload of the reply.
       * @param metadata The metadata for the message.
       * @param <T> The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <T> Effect<T> reply(T message, Metadata metadata);

      /**
       * Create a forward reply.
       *
       * @param serviceCall The service call representing the forward.
       * @param <T> The type of the message that must be returned by this call.
       * @return A forward reply.
       */
      <T> Effect<T> forward(DeferredCall<? extends Object, T> serviceCall);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @param <T> The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <T> Effect<T> error(String description);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @param grpcErrorCode A custom gRPC status code.
       * @param <T> The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <T> Effect<T> error(String description, Status.Code grpcErrorCode);

      /**
       * Create an error reply with a custom status code.
       * This status code will be translated to an HTTP or gRPC code
       * depending on the type of service being exposed.
       *
       * @param description The description of the error.
       * @param httpErrorCode A custom Kalix status code to represent the error.
       * @return An error reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> error(String description, StatusCode.ErrorCode httpErrorCode);
    }

    interface OnSuccessBuilder<S> {

      /**
       * Reply after for example <code>updateState</code>.
       *
       * @param message The payload of the reply.
       * @param <T> The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <T> Effect<T> thenReply(T message);

      /**
       * Reply after for example <code>updateState</code>.
       *
       * @param message The payload of the reply.
       * @param metadata The metadata for the message.
       * @param <T> The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <T> Effect<T> thenReply(T message, Metadata metadata);

      /**
       * Create a forward reply after for example <code>updateState</code>.
       *
       * @param serviceCall The service call representing the forward.
       * @param <T> The type of the message that must be returned by this call.
       * @return A forward reply.
       */
      <T> Effect<T> thenForward(DeferredCall<? extends Object, T> serviceCall);
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
