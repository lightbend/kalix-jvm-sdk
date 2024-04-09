/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.action

import akka.NotUsed
import akka.stream.javadsl.Source
import kalix.javasdk.action.Action
import kalix.javasdk.action.ActionContext
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.impl.action.ActionRouter.HandlerNotFound

import java.util.Optional

object ActionRouter {
  case class HandlerNotFound(commandName: String) extends RuntimeException
}
abstract class ActionRouter[A <: Action](protected val action: A) {

  /**
   * Handle a unary call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param message
   *   The message envelope of the message.
   * @param context
   *   The action context.
   * @return
   *   A future of the message to return.
   */
  final def handleUnary(commandName: String, message: MessageEnvelope[Any], context: ActionContext): Action.Effect[_] =
    callWithContext(context) { () =>
      handleUnary(commandName, message)
    }

  /**
   * Handle a unary call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param message
   *   The message envelope of the message.
   * @return
   *   A future of the message to return.
   */
  def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[_]

  /**
   * Handle a streamed out call call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param message
   *   The message envelope of the message.
   * @param context
   *   The action context.
   * @return
   *   The stream of messages to return.
   */
  final def handleStreamedOut(
      commandName: String,
      message: MessageEnvelope[Any],
      context: ActionContext): Source[Action.Effect[_], NotUsed] =
    callWithContext(context) { () =>
      handleStreamedOut(commandName, message)
    }

  /**
   * Handle a streamed out call call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param message
   *   The message envelope of the message.
   * @return
   *   The stream of messages to return.
   */
  def handleStreamedOut(commandName: String, message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed]

  /**
   * Handle a streamed in call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param stream
   *   The stream of messages to handle.
   * @param context
   *   The action context.
   * @return
   *   A future of the message to return.
   */
  final def handleStreamedIn(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed],
      context: ActionContext): Action.Effect[_] =
    callWithContext(context) { () =>
      handleStreamedIn(commandName, stream)
    }

  /**
   * Handle a streamed in call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param stream
   *   The stream of messages to handle.
   * @return
   *   A future of the message to return.
   */
  def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_]

  /**
   * Handle a full duplex streamed in call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param stream
   *   The stream of messages to handle.
   * @param context
   *   The action context.
   * @return
   *   The stream of messages to return.
   */
  final def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed],
      context: ActionContext): Source[Action.Effect[_], NotUsed] =
    callWithContext(context) { () =>
      handleStreamed(commandName, stream)
    }

  /**
   * Handle a full duplex streamed in call.
   *
   * @param commandName
   *   The name of the command this call is for.
   * @param stream
   *   The stream of messages to handle.
   * @return
   *   The stream of messages to return.
   */
  def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed]

  private def callWithContext[T](context: ActionContext)(func: () => T) = {
    // only set, never cleared, to allow access from other threads in async callbacks in the action
    // the same handler and action instance is expected to only ever be invoked for a single command
    action._internalSetActionContext(Optional.of(context))
    try {
      func()
    } catch {
      case HandlerNotFound(name) =>
        throw new RuntimeException(s"No call handler found for call $name on ${action.getClass.getName}")
    }
  }

  def actionClass(): Class[_] = action.getClass
}
