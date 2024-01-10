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

package kalix.javasdk.impl.action

import akka.NotUsed
import akka.stream.javadsl.Source
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.action.Action
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.impl.AnySupport.ProtobufEmptyTypeUrl
import kalix.javasdk.impl.CommandHandler
import kalix.javasdk.impl.InvocationContext

// TODO: abstract away reactor dependency
import reactor.core.publisher.Flux

class ReflectiveActionRouter[A <: Action](
    action: A,
    commandHandlers: Map[String, CommandHandler],
    ignoreUnknown: Boolean)
    extends ActionRouter[A](action) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[_] = {

    val commandHandler = commandHandlerLookup(commandName)

    val invocationContext =
      InvocationContext(
        message.payload().asInstanceOf[ScalaPbAny],
        commandHandler.requestMessageDescriptor,
        message.metadata())

    val inputTypeUrl = message.payload().asInstanceOf[ScalaPbAny].typeUrl
    val methodInvoker = commandHandler.lookupInvoker(inputTypeUrl)

    methodInvoker match {
      case Some(invoker) =>
        inputTypeUrl match {
          case ProtobufEmptyTypeUrl =>
            invoker
              .invoke(action)
              .asInstanceOf[Action.Effect[_]]
          case _ =>
            invoker
              .invoke(action, invocationContext)
              .asInstanceOf[Action.Effect[_]]
        }
      case None if ignoreUnknown => ActionEffectImpl.Builder.ignore()
      case None =>
        throw new NoSuchElementException(
          s"Couldn't find any method with input type [$inputTypeUrl] in Action [$action].")

    }
  }

  override def handleStreamedOut(
      commandName: String,
      message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {

    val componentMethod = commandHandlerLookup(commandName)

    val context =
      InvocationContext(
        message.payload().asInstanceOf[ScalaPbAny],
        componentMethod.requestMessageDescriptor,
        message.metadata())

    val inputTypeUrl = message.payload().asInstanceOf[ScalaPbAny].typeUrl
    componentMethod.lookupInvoker(inputTypeUrl) match {
      case Some(methodInvoker) =>
        val response = methodInvoker.invoke(action, context).asInstanceOf[Flux[Action.Effect[_]]]
        Source.fromPublisher(response)
      case None if ignoreUnknown => Source.empty()
      case None =>
        throw new NoSuchElementException(
          s"Couldn't find any method with input type [$inputTypeUrl] in Action [$action].")
    }
  }

  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] =
    throw new IllegalArgumentException("Stream in calls are not supported")

  // TODO: to implement
  override def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] =
    throw new IllegalArgumentException("Stream in calls are not supported")
}
