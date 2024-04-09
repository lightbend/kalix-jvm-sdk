/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.action

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.action.ActionEffectImpl
import io.grpc.Status
import kalix.javasdk.impl
import kalix.javasdk.impl.action.ActionContextImpl
import kalix.scalasdk.impl.MetadataImpl
import kalix.scalasdk.impl.action.ScalaActionContextAdapter
import kalix.scalasdk.timer.TimerScheduler
import kalix.scalasdk.impl.timer.TimerSchedulerImpl

object Action {

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled. You can think of it as a set of
   * instructions you are passing to Kalix. Kalix will process the instructions on your behalf.
   *
   * Each Kalix component defines its own effects, which are a set of predefined operations that match the capabilities
   * of that component.
   *
   * An Action Effect can either:
   *
   *   - reply with a message to the caller
   *   - reply with a message to be published to a topic (in case the method is a publisher)
   *   - forward the message to another component
   *   - return an error
   *   - ignore the call
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
       * Create a message reply with custom Metadata.
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
       * Create a message reply from an async operation result with custom Metadata.
       *
       * @param message
       *   The future payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @return
       *   A message reply.
       * @tparam S
       *   The type of the message that must be returned by this call.
       */
      def asyncReply[S](message: Future[S], metadata: Metadata): Action.Effect[S]

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

/**
 * Actions are stateless components that can be used to implement different uses cases, such as:
 *
 *   - a pure function.
 *   - request conversion - you can use Actions to convert incoming data into a different format before forwarding a
 *     call to a different component.
 *   - publish messages to a Topic.
 *   - subscribe to events from an Event Sourced Entity.
 *   - subscribe to state changes from a Value Entity.
 *   - schedule and cancel Timers.
 *
 * Actions can be triggered in multiple ways. For example, by:
 *
 *   - a gRPC service call.
 *   - an HTTP service call.
 *   - a forwarded call from another component.
 *   - a scheduled call from a Timer.
 *   - an incoming message from a Topic.
 *   - an incoming event from an Event Sourced Entity, from within the same service or from a different service.
 *   - state changes notification from a Value Entity on the same service.
 *
 * An Action method should return an [[kalix.scalasdk.action.Action.Effect]] that describes what to do next.
 */
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
   * Returns a [[kalix.scalasdk.timer.TimerScheduler]] that can be used to schedule further in time.
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

    new TimerSchedulerImpl(
      javaActionContextImpl.messageCodec,
      javaActionContextImpl.system,
      MetadataImpl(javaActionContextImpl.metadata.asInstanceOf[impl.MetadataImpl]))
  }

  protected final def effects[T]: Action.Effect.Builder =
    ActionEffectImpl.builder()
}
