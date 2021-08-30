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

package com.akkaserverless.javasdk.impl.action

import java.util.Optional
import java.util.concurrent.CompletionStage

import akka.NotUsed
import akka.stream.javadsl.Source
import com.akkaserverless.javasdk.Reply
import com.akkaserverless.javasdk.action.Action
import com.akkaserverless.javasdk.action.ActionContext
import com.akkaserverless.javasdk.action.MessageEnvelope
import com.akkaserverless.javasdk.impl.EntityExceptions
import com.akkaserverless.javasdk.impl.action.ActionHandler.HandlerNotFound

object ActionHandler {
  case class HandlerNotFound(val commandName: String) extends RuntimeException
}
abstract class ActionHandler[A <: Action](protected val action: A) {

  /**
   * Handle a unary call.
   *
   * @param commandName The name of the command this call is for.
   * @param message     The message envelope of the message.
   * @param context     The action context.
   * @return A future of the message to return.
   */
  @throws[Throwable]
  final def handleUnary(commandName: String,
                        message: MessageEnvelope[Any],
                        context: ActionContext): CompletionStage[Reply[Any]] =
    callWithContext(context) { () =>
      handleUnary(commandName, message)
    }

  /**
   * Handle a unary call.
   *
   * @param commandName The name of the command this call is for.
   * @param message     The message envelope of the message.
   * @return A future of the message to return.
   */
  @throws[Throwable]
  def handleUnary(commandName: String, message: MessageEnvelope[Any]): CompletionStage[Reply[Any]]

  /**
   * Handle a streamed out call call.
   *
   * @param commandName The name of the command this call is for.
   * @param message     The message envelope of the message.
   * @param context     The action context.
   * @return The stream of messages to return.
   */
  final def handleStreamedOut(commandName: String,
                              message: MessageEnvelope[Any],
                              context: ActionContext): Source[Reply[Any], NotUsed] =
    callWithContext(context) { () =>
      handleStreamedOut(commandName, message)
    }

  /**
   * Handle a streamed out call call.
   *
   * @param commandName The name of the command this call is for.
   * @param message     The message envelope of the message.
   * @return The stream of messages to return.
   */
  def handleStreamedOut(commandName: String, message: MessageEnvelope[Any]): Source[Reply[Any], NotUsed]

  /**
   * Handle a streamed in call.
   *
   * @param commandName The name of the command this call is for.
   * @param stream      The stream of messages to handle.
   * @param context     The action context.
   * @return A future of the message to return.
   */
  final def handleStreamedIn(commandName: String,
                             stream: Source[MessageEnvelope[Any], NotUsed],
                             context: ActionContext): CompletionStage[Reply[Any]] =
    callWithContext(context) { () =>
      handleStreamedIn(commandName, stream)
    }

  /**
   * Handle a streamed in call.
   *
   * @param commandName The name of the command this call is for.
   * @param stream      The stream of messages to handle.
   * @return A future of the message to return.
   */
  def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): CompletionStage[Reply[Any]]

  /**
   * Handle a full duplex streamed in call.
   *
   * @param commandName The name of the command this call is for.
   * @param stream      The stream of messages to handle.
   * @param context     The action context.
   * @return The stream of messages to return.
   */
  final def handleStreamed(commandName: String,
                           stream: Source[MessageEnvelope[Any], NotUsed],
                           context: ActionContext): Source[Reply[Any], NotUsed] =
    callWithContext(context) { () =>
      handleStreamed(commandName, stream)
    }

  /**
   * Handle a full duplex streamed in call.
   *
   * @param commandName The name of the command this call is for.
   * @param stream      The stream of messages to handle.
   * @return The stream of messages to return.
   */
  def handleStreamed(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Source[Reply[Any], NotUsed]

  private def callWithContext[T](context: ActionContext)(func: () => T) = {
    action.setActionContext(Optional.of(context))
    try {
      func()
    } catch {
      case HandlerNotFound(name) =>
        throw new RuntimeException(
          s"No call handler found for call $name on ${action.getClass.getName}"
        )
    } finally {
      action.setActionContext(Optional.empty())
    }
  }
}
