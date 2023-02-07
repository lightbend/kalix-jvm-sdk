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

package kalix.scalasdk.eventsourcedentity

import kalix.scalasdk.{ Context, DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import io.grpc.Status
import kalix.scalasdk.ErrorCode

object EventSourcedEntity {

  /**
   * A return type to allow returning forwards or failures, and attaching effects to messages.
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

      /**
       * Create an error reply with a custom status code. This status code will be translated to an HTTP or gRPC code
       * depending on the type of service being exposed.
       *
       * @param description
       *   The description of the error.
       * @param errorCode
       *   A custom Kalix status code to represent the error.
       * @return
       *   An error reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def error[T](description: String, errorCode: ErrorCode): Effect[T]
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

/** @tparam S The type of the state for this entity. */
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
