/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.valueentity

import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.valueentity.ValueEntityEffectImpl
import io.grpc.Status

object ValueEntity {
  object Effect {

    /**
     * Construct the effect that is returned by the command handler. The effect describes next processing actions, such
     * as updating state and sending a reply.
     *
     * @tparam S
     *   The type of the state for this entity.
     */
    trait Builder[S] {

      def updateState(newState: S): OnSuccessBuilder[S]

      /**
       * Delete the entity. No additional updates are allowed afterwards.
       */
      def deleteEntity(): OnSuccessBuilder[S]

      /**
       * Delete the entity. No additional updates are allowed afterwards.
       */
      @deprecated("Renamed to deleteEntity", "1.1.5")
      def deleteState(): OnSuccessBuilder[S]

      /**
       * Create a message reply.
       *
       * @param message
       *   The payload of the reply.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def reply[T](message: T): Effect[T]

      /**
       * Create a message reply.
       *
       * @param message
       *   The payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def reply[T](message: T, metadata: Metadata): Effect[T]

      /**
       * Create a forward reply.
       *
       * @param serviceCall
       *   The service call representing the forward.
       * @return
       *   A forward reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def forward[T](serviceCall: DeferredCall[_, T]): Effect[T]

      /**
       * Create an error reply.
       *
       * @param description
       *   The description of the error.
       * @return
       *   An error reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def error[T](description: String): Effect[T]

      /**
       * Create an error reply.
       *
       * @param description
       *   The description of the error.
       * @param statusCode
       *   A gRPC status code.
       * @return
       *   An error reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def error[T](description: String, statusCode: Status.Code): Effect[T]
    }

    trait OnSuccessBuilder[S] {

      /**
       * Reply after for example `updateState`.
       *
       * @param message
       *   The payload of the reply.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenReply[T](message: T): Effect[T]

      /**
       * Reply after for example <code>updateState</code>.
       *
       * @param message
       *   The payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenReply[T](message: T, metadata: Metadata): Effect[T]

      /**
       * Create a forward reply after for example <code>updateState</code>.
       *
       * @param serviceCall
       *   The service call representing the forward.
       * @return
       *   A forward reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenForward[T](serviceCall: DeferredCall[_, T]): Effect[T]
    }
  }

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled. You can think of it as a set of
   * instructions you are passing to Kalix. Kalix will process the instructions on your behalf and ensure that any data
   * that needs to be persisted will be persisted.
   *
   * Each Kalix component defines its own effects, which are a set of predefined operations that match the capabilities
   * of that component.
   *
   * A ValueEntity Effect can either:
   *
   *   - update the entity state and send a reply to the caller
   *   - directly reply to the caller if the command is not requesting any state change
   *   - rejected the command by returning an error
   *   - instruct Kalix to delete the entity
   *
   * @tparam T
   *   The type of the message that must be returned by this call.
   */
  trait Effect[T] {

    /**
     * Attach the given side effects to this reply.
     *
     * @param sideEffects
     *   The effects to attach.
     * @return
     *   A new reply with the attached effects.
     */
    def addSideEffects(sideEffects: Seq[SideEffect]): Effect[T]
  }

}

/**
 * Value Entities persist their state on every change. You can think of them as a Key-Value entity where the key is the
 * entity id and the value is the state of the entity.
 *
 * Kalix Value Entities have nothing in common with the domain-driven design concept of Value Objects. The Value in the
 * name refers to the direct modification of the entity's state.
 *
 * When implementing a Value Entity, you first define what will be its internal state (your domain model), and the
 * commands it will handle (mutation requests).
 *
 * Each command is handled by a command handler. Command handlers are methods returning an
 * [[kalix.scalasdk.valueentity.ValueEntity.Effect]]. When handling a command, you use the Effect API to:
 *
 *   - update the entity state and send a reply to the caller
 *   - directly reply to the caller if the command is not requesting any state change
 *   - rejected the command by returning an error
 *   - instruct Kalix to delete the entity
 *
 * @tparam S
 *   The type of the state for this entity.
 */
abstract class ValueEntity[S] {
  private var _commandContext: Option[CommandContext] = None

  /**
   * Implement by returning the initial empty state object. This object will be passed into the command handlers, until
   * a new state replaces it.
   *
   * <p>Also known as "zero state" or "neutral state".
   *
   * <p><code>null</code> is an allowed value.
   */
  def emptyState: S

  /**
   * Additional context and metadata for a command handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected final def commandContext(): CommandContext = {
    try {
      _commandContext.get
    } catch {
      case _: NoSuchElementException =>
        throw new IllegalStateException("CommandContext is only available when handling a command.")
    }
  }

  /** INTERNAL API */
  final def _internalSetCommandContext(context: Option[CommandContext]): Unit = {
    _commandContext = context
  }

  protected final def effects: ValueEntity.Effect.Builder[S] =
    ValueEntityEffectImpl[S]()

}
