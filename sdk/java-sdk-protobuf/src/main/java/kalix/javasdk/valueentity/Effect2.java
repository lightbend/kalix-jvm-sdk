package kalix.javasdk.valueentity;

import io.grpc.Status;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.SideEffect;
import kalix.javasdk.StatusCode;

import java.util.Collection;

public interface Effect2<T> {

  /**
   * Construct the effect that is returned by the command handler. The effect describes next
   * processing actions, such as updating state and sending a reply.
   *
   * @param <S> The type of the state for this entity.
   */
  interface Builder<S> {

    ValueEntity.Effect.OnSuccessBuilder<S> updateState(S newState);

    /**
     * Delete the entity. No additional updates are allowed afterwards.
     */
    ValueEntity.Effect.OnSuccessBuilder<S> deleteEntity();

    /**
     * Delete the entity. No additional updates are allowed afterwards.
     *
     * @deprecated Renamed to deleteEntity
     */
    @Deprecated(since = "1.1.5", forRemoval = true)
    ValueEntity.Effect.OnSuccessBuilder<S> deleteState();

    /**
     * Create a message reply.
     *
     * @param message The payload of the reply.
     * @param <T>     The type of the message that must be returned by this call.
     * @return A message reply.
     */
    <T> ValueEntity.Effect<T> reply(T message);

    /**
     * Create a message reply.
     *
     * @param message  The payload of the reply.
     * @param metadata The metadata for the message.
     * @param <T>      The type of the message that must be returned by this call.
     * @return A message reply.
     */
    <T> ValueEntity.Effect<T> reply(T message, Metadata metadata);

    /**
     * Create a forward reply.
     *
     * @param serviceCall The service call representing the forward.
     * @param <T>         The type of the message that must be returned by this call.
     * @return A forward reply.
     */
    <T> ValueEntity.Effect<T> forward(DeferredCall<? extends Object, T> serviceCall);

    /**
     * Create an error reply.
     *
     * @param description The description of the error.
     * @param <T>         The type of the message that must be returned by this call.
     * @return An error reply.
     */
    <T> ValueEntity.Effect<T> error(String description);

    /**
     * Create an error reply.
     *
     * @param description The description of the error.
     * @param statusCode  A custom gRPC status code.
     * @param <T>         The type of the message that must be returned by this call.
     * @return An error reply.
     */
    <T> ValueEntity.Effect<T> error(String description, Status.Code grpcErrorCode);

    /**
     * Create an error reply with a custom status code.
     * This status code will be translated to an HTTP or gRPC code
     * depending on the type of service being exposed.
     *
     * @param description The description of the error.
     * @param errorCode   A custom Kalix status code to represent the error.
     * @param <T>         The type of the message that must be returned by this call.
     * @return An error reply.
     */
    <T> ValueEntity.Effect<T> error(String description, StatusCode.ErrorCode httpErrorCode);
  }

  interface OnSuccessBuilder<S> {

    /**
     * Reply after for example <code>updateState</code>.
     *
     * @param message The payload of the reply.
     * @param <T>     The type of the message that must be returned by this call.
     * @return A message reply.
     */
    <T> ValueEntity.Effect<T> thenReply(T message);

    /**
     * Reply after for example <code>updateState</code>.
     *
     * @param message  The payload of the reply.
     * @param metadata The metadata for the message.
     * @param <T>      The type of the message that must be returned by this call.
     * @return A message reply.
     */
    <T> ValueEntity.Effect<T> thenReply(T message, Metadata metadata);

    /**
     * Create a forward reply after for example <code>updateState</code>.
     *
     * @param serviceCall The service call representing the forward.
     * @param <T>         The type of the message that must be returned by this call.
     * @return A forward reply.
     */
    <T> ValueEntity.Effect<T> thenForward(DeferredCall<? extends Object, T> serviceCall);
  }

  /**
   * Attach the given side effects to this reply.
   *
   * @param sideEffects The effects to attach.
   * @return A new reply with the attached effects.
   */
  ValueEntity.Effect<T> addSideEffects(Collection<SideEffect> sideEffects);

  /**
   * Attach the given effects to this reply.
   *
   * @param effects The effects to attach.
   * @return A new reply with the attached effects.
   */
  ValueEntity.Effect<T> addSideEffects(SideEffect... effects);
}

