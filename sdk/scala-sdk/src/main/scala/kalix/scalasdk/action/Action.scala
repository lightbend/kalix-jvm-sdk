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

package kalix.scalasdk.action

import scala.collection.immutable.Seq
import scala.concurrent.Future
import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.action.ActionEffectImpl
import io.grpc.Status

object Action {

  /**
   * A return type to allow returning forwards or failures, and attaching effects to messages.
   *
   * @tparam T
   *   The type of the message that must be returned by this call.
   */
  trait Effect[+T] {

    /**
     * Attach the given side effects to this reply.
     *
     * @param sideEffects
     *   The effects to attach.
     * @return
     *   A new reply with the attached effects.
     */
    def addSideEffect(sideEffects: SideEffect*): Action.Effect[T]

    def addSideEffects(sideEffects: Seq[SideEffect]): Action.Effect[T]
  }

  /**
   * Construct the effect that is returned by the command handler. The effect describes next processing actions, such as
   * sending a reply.
   */
  object Effect {
    trait Builder {

      /**
       * Create a message reply.
       *
       * @param message
       *   The payload of the reply.
       * @return
       *   A message reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def reply[S](message: S): Action.Effect[S]

      /**
       * Create a message reply.
       *
       * @param message
       *   The payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @return
       *   A message reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def reply[S](message: S, metadata: Metadata): Action.Effect[S]

      /**
       * Create a forward reply.
       *
       * @param serviceCall
       *   The service call representing the forward.
       * @return
       *   A forward reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def forward[S](serviceCall: DeferredCall[_, S]): Action.Effect[S]

      /**
       * Create a reply that contains neither a message nor a forward nor an error.
       *
       * @return
       *   The reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def noReply[S]: Action.Effect[S]

      /**
       * Create an error reply.
       *
       * @param description
       *   The description of the error.
       * @return
       *   An error reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def error[S](description: String): Action.Effect[S]

      /**
       * Create an error reply.
       *
       * @param description
       *   The description of the error.
       * @param statusCode
       *   A gRPC status code.
       * @return
       *   An error reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def error[S](description: String, statusCode: Status.Code): Action.Effect[S]

      /**
       * Create a message reply from an async operation result.
       *
       * @param message
       *   The future payload of the reply.
       * @return
       *   A message reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def asyncReply[S](message: Future[S]): Action.Effect[S]

      /**
       * Create a reply from an async operation result returning an effect.
       *
       * @param futureEffect
       *   The future effect to reply with.
       * @return
       *   A reply, the actual type depends on the nested Effect.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def asyncEffect[S](futureEffect: Future[Action.Effect[S]]): Action.Effect[S]

    }
  }
}
abstract class Action {
  @volatile
  private var _actionContext: Option[ActionContext] = None

  /**
   * Additional context and metadata for a message handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected final def actionContext: ActionContext =
    actionContext("ActionContext is only available when handling a message.")

  /**
   * INTERNAL API
   *
   * Same as actionContext, but if specific error message when accessing components.
   */
  protected final def contextForComponents: ActionContext =
    actionContext("Components can only be accessed when handling a message.")

  private def actionContext(errorMessage: String): ActionContext =
    _actionContext.getOrElse(throw new IllegalStateException(errorMessage))

  /** INTERNAL API */
  final def _internalSetActionContext(context: Option[ActionContext]): Unit = {
    _actionContext = context
  }

  protected final def effects[T]: Action.Effect.Builder =
    ActionEffectImpl.builder()
}
