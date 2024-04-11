/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.eventsourcedentity

import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import io.grpc.Status

object EventSourcedEntity {

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled. You can think of it as a set of
   * instructions you are passing to Kalix. Kalix will process the instructions on your behalf and ensure that any data
   * that needs to be persisted will be persisted.
   *
   * Each Kalix component defines its own effects, which are a set of predefined operations that match the capabilities
   * of that component.
   *
   * An EventSourcedEntity Effect can either:
   *
   *   - emit events and send a reply to the caller
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

  object Effect {

    /**
     * Construct the effect that is returned by the command handler. The effect describes next processing actions, such
     * as emitting events and sending a reply.
     *
     * @tparam S
     *   The type of the state for this entity.
     */
    trait Builder[S] {

      def emitEvent(event: Object): OnSuccessBuilder[S]

      def emitEvents(event: List[_]): OnSuccessBuilder[S]

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
       * Delete the entity. No addition events are allowed.
       */
      def deleteEntity(): OnSuccessBuilder[S]

      /**
       * Reply after for example <code>emitEvent</code>.
       *
       * @param replyMessage
       *   Function to create the reply message from the new state.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenReply[T](replyMessage: S => T): Effect[T]

      /**
       * Reply after for example <code>emitEvent</code>.
       *
       * @param replyMessage
       *   Function to create the reply message from the new state.
       * @param metadata
       *   The metadata for the message.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenReply[T](replyMessage: S => T, metadata: Metadata): Effect[T]

      /**
       * Create a forward reply after for example <code>emitEvent</code>.
       *
       * @param serviceCall
       *   The service call representing the forward.
       * @return
       *   A forward reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenForward[T](serviceCall: S => DeferredCall[_, T]): Effect[T]

      /**
       * Attach the given side effect to this reply from the new state.
       *
       * @param sideEffect
       *   The effect to attach.
       * @return
       *   A new reply with the attached effect.
       */
      def thenAddSideEffect(sideEffect: S => SideEffect): OnSuccessBuilder[S]
    }
  }
}

/**
 * The Event Sourced state model captures changes to data by storing events in a journal. The current entity state is
 * derived from the emitted events.
 *
 * When implementing an Event Sourced Entity, you first define what will be its internal state (your domain model), the
 * commands it will handle (mutation requests) and the events it will emit (state changes).
 *
 * Each command is handled by a command handler. Command handlers are methods returning an
 * [[kalix.scalasdk.eventsourcedentity.EventSourcedEntity.Effect]]. When handling a command, you use the Effect API to:
 *
 *   - emit events and build a reply
 *   - directly returning to the caller if the command is not requesting any state change
 *   - rejected the command by returning an error
 *   - instruct Kalix to delete the entity
 *
 * Each event is handled by an event handler method and should return an updated state for the entity.
 * @tparam S
 *   The type of the state for this entity.
 */
abstract class EventSourcedEntity[S] {
  private var _commandContext: Option[CommandContext] = None
  private var _eventContext: Option[EventContext] = None

  /**
   * Implement by returning the initial empty state object. This object will be passed into the command and event
   * handlers, until a new state replaces it.
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

  /**
   * Additional context and metadata for a command handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected final def eventContext(): EventContext = {
    try {
      _eventContext.get
    } catch {
      case _: NoSuchElementException =>
        throw new IllegalStateException("EventContext is only available when handling an event.")
    }
  }

  /** INTERNAL API */
  final def _internalSetEventContext(context: Option[EventContext]): Unit = {
    _eventContext = context
  }

  protected final def effects: EventSourcedEntity.Effect.Builder[S] =
    EventSourcedEntityEffectImpl[Any, S]()

}
