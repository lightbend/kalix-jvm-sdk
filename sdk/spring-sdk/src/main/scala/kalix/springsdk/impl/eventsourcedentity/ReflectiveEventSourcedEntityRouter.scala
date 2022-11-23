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

package kalix.springsdk.impl.eventsourcedentity

import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.Metadata
import kalix.javasdk.eventsourcedentity.{ CommandContext, EventSourcedEntity }
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter
import kalix.springsdk.impl.MethodInvoker
import kalix.springsdk.impl.SpringSdkMessageCodec
import kalix.springsdk.impl.{ CommandHandler, InvocationContext }

class ReflectiveEventSourcedEntityRouter[S, E <: EventSourcedEntity[S]](
    override protected val entity: E,
    commandHandlers: Map[String, CommandHandler],
    eventHandlerMethods: Map[String, MethodInvoker],
    messageCodec: SpringSdkMessageCodec)
    extends EventSourcedEntityRouter[S, E](entity) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(
      commandName,
      throw new HandlerNotFoundException("command", commandName, commandHandlers.keySet))

  private def eventHandlerLookup(eventName: String) =
    eventHandlerMethods.getOrElse(
      eventName,
      throw new HandlerNotFoundException("event", eventName, commandHandlers.keySet))

  override def handleEvent(state: S, event: Any): S = {

    entity._internalSetCurrentState(state)

    event match {
      case s: ScalaPbAny => // replaying event coming from proxy
        // FIXME: where should we get the metadata from here?
        val invocationContext = InvocationContext(s, JavaPbAny.getDescriptor, Metadata.EMPTY)

        eventHandlerLookup(s.typeUrl)
          .invoke(entity, invocationContext)
          .asInstanceOf[S]

      case _ => // processing runtime event coming from memory
        val typeName = messageCodec.typeUrlFor(event.getClass)

        eventHandlerLookup(typeName).method
          .invoke(entity, event.asInstanceOf[event.type])
          .asInstanceOf[S]
    }
  }

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      commandContext: CommandContext): EventSourcedEntity.Effect[_] = {

    entity._internalSetCurrentState(state)

    val commandHandler = commandHandlerLookup(commandName)
    val invocationContext =
      InvocationContext(
        command.asInstanceOf[ScalaPbAny],
        commandHandler.requestMessageDescriptor,
        commandContext.metadata())

    val inputTypeUrl = command.asInstanceOf[ScalaPbAny].typeUrl

    commandHandler
      .getInvoker(inputTypeUrl)
      .invoke(entity, invocationContext)
      .asInstanceOf[EventSourcedEntity.Effect[_]]
  }
}

final class HandlerNotFoundException(handlerType: String, name: String, availableHandlers: Set[String])
    extends RuntimeException(
      s"no matching $handlerType handler for '$name'. " +
      s"Available handlers are: [${availableHandlers.mkString(", ")}]")
