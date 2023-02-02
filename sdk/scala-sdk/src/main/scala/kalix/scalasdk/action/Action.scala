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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.action.ActionEffectImpl
import io.grpc.Status
import kalix.javasdk.StatusCode.ErrorCode
import kalix.javasdk.impl.action.ActionContextImpl
import kalix.scalasdk.impl.action.ScalaActionContextAdapter
import kalix.scalasdk.timer.TimerScheduler
import kalix.scalasdk.impl.timer.TimerSchedulerImpl

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

    /**
     * @return
     *   true if this effect supports attaching side effects, if returning false addSideEffects will throw an exception.
     */
    def canHaveSideEffects: Boolean
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
      @deprecated
      def error[S](description: String, statusCode: Status.Code): Action.Effect[S]

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
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def error[S](description: String, errorCode: ErrorCode): Action.Effect[S]

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

      /**
       * Ignore the current element and proceed with processing the next element if returned for an element from
       * eventing in. If used as a response to a regular gRPC or HTTP request it is turned into a NotFound response.
       *
       * Ignore is not allowed to have side effects added with `addSideEffects`
       */
      def ignore[S]: Action.Effect[S]
    }
  }
}

abstract class Action {
  @volatile
  private var _actionContext: Option[ActionContext] = None

  /**
   * An ExecutionContext to use when composing Futures inside Actions.
   */
  implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  /**
   * Additional context and metadata for a message handler.
   *
   * It will throw an exception if accessed from constructor.
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

  /**
   * Returns a [[TimerScheduler]] that can be used to schedule further in time.
   */
  final def timers: TimerScheduler = {

    val javaActionContextImpl =
      actionContext("Timers can only be scheduled or cancelled when handling a message.") match {
        case ScalaActionContextAdapter(actionContext: ActionContextImpl) => actionContext
        // should not happen as we always need to pass ScalaActionContextAdapter(ActionContextImpl)
        case other =>
          throw new RuntimeException(
            s"Incompatible ActionContext instance. Found ${other.getClass}, expecting ${classOf[ActionContextImpl].getName}")
      }

    new TimerSchedulerImpl(javaActionContextImpl.messageCodec, javaActionContextImpl.system)
  }

  protected final def effects[T]: Action.Effect.Builder =
    ActionEffectImpl.builder()
}
