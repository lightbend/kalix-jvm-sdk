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

package kalix.javasdk.replicatedentity;

import kalix.javasdk.Metadata;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.SideEffect;
import kalix.javasdk.StatusCode;
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityEffectImpl;
import kalix.replicatedentity.ReplicatedData;
import io.grpc.Status;

import java.util.Collection;
import java.util.Optional;

/** @param <D> The replicated data type for this entity. */
public abstract class ReplicatedEntity<D extends ReplicatedData> {

  private Optional<CommandContext> commandContext = Optional.empty();

  /**
   * Implement by returning the initial empty replicated data object. This object will be passed
   * into the command handlers.
   *
   * <p>Also known as the "zero" or "neutral" state.
   *
   * <p>The initial data cannot be <code>null</code>.
   *
   * @param factory the factory to create the initial empty replicated data object
   */
  public abstract D emptyData(ReplicatedDataFactory factory);

  /**
   * Additional context and metadata for a command handler.
   *
   * <p>It will throw an exception if accessed from the entity constructor.
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

  protected final <R> Effect.Builder<D> effects() {
    return new ReplicatedEntityEffectImpl<D, R>();
  }

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled.
   * You can think of it as a set of instructions you are passing to Kalix. Kalix will process the instructions on your
   * behalf and ensure that any data that needs to be persisted will be persisted.
   * <p>
   * Each Kalix component defines its own effects, which are a set of predefined
   * operations that match the capabilities of that component.
   * <p>
   * A Replicated Effect can either:
   * <p>
   * <ul>
   *   <li>update the entity state and send a reply to the caller
   *   <li>directly reply to the caller if the command is not requesting any state change
   *   <li>rejected the command by returning an error
   *   <li>instruct Kalix to delete the entity
   * </ul>
   *
   * @param <R> The type of the reply message that must be returned by this call.
   */
  public interface Effect<R> {

    /**
     * Construct the effect that is returned by the command handler. The effect describes next
     * processing actions, such as sending a reply or deleting an entity.
     *
     * @param <D> The replicated data type for this entity.
     */
    interface Builder<D> {

      /** Update the underlying replicated data for the replicated entity. */
      OnSuccessBuilder update(D newData);

      /**
       * Delete the replicated entity.
       *
       * <p>When a replicated entity is deleted, it may not be created again. Additionally,
       * replicated entity deletion results in tombstones that get accumulated for the life of the
       * cluster. If you expect to delete replicated entities frequently, it's recommended that you
       * store them in a single or sharded {@link ReplicatedMap}, rather than individual replicated
       * entities.
       */
      OnSuccessBuilder delete();

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
       * @param grpcErrorCode A custom gRPC status code.
       * @return An error reply.
       * @param <T> The type of the message that must be returned by this call.
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

    interface OnSuccessBuilder {

      /**
       * Reply after for example <code>delete</code>.
       *
       * @param message The payload of the reply.
       * @return A message reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> thenReply(T message);

      /**
       * Reply after for example <code>delete</code>.
       *
       * @param message The payload of the reply.
       * @param metadata The metadata for the message.
       * @return A message reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> thenReply(T message, Metadata metadata);

      /**
       * Create a forward reply after for example <code>delete</code>.
       *
       * @param serviceCall The service call representing the forward.
       * @return A forward reply.
       * @param <T> The type of the message that must be returned by this call.
       */
      <T> Effect<T> thenForward(DeferredCall<? extends Object, T> serviceCall);
    }

    /**
     * Attach the given side effects to this reply.
     *
     * @param sideEffects The effects to attach.
     * @return A new reply with the attached effects.
     */
    Effect<R> addSideEffects(Collection<SideEffect> sideEffects);

    /**
     * Attach the given effects to this reply.
     *
     * @param effects The effects to attach.
     * @return A new reply with the attached effects.
     */
    Effect<R> addSideEffects(SideEffect... effects);
  }
}
