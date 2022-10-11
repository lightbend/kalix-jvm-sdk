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
import kalix.javasdk.eventsourcedentity.{ CommandContext, EventSourcedEntity }
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter
import kalix.springsdk.impl.{ CommandHandler, InvocationContext }

import java.lang.reflect.Method

class ReflectiveEventSourcedEntityRouter[S, E <: EventSourcedEntity[S]](
    override protected val entity: E,
    commandHandlers: Map[String, CommandHandler],
    eventHandlerMethods: Map[Class[_], Method])
    extends EventSourcedEntityRouter[S, E](entity) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  private def eventHandlerLookup(eventClass: Class[_]) =
    eventHandlerMethods.getOrElse(eventClass, throw new RuntimeException(s"no matching handler for '$eventClass'"))

  override def handleEvent(state: S, event: Any): S = {
    entity._internalSetCurrentState(state)

    eventHandlerLookup(event.getClass)
      .invoke(entity, event.asInstanceOf[event.type])
      .asInstanceOf[S]
  }

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: CommandContext): EventSourcedEntity.Effect[_] = {

    val commandHandler = commandHandlerLookup(commandName)
    val invocationContext =
      InvocationContext(command.asInstanceOf[ScalaPbAny], commandHandler.requestMessageDescriptor)

    val inputTypeUrl = command.asInstanceOf[ScalaPbAny].typeUrl
    val methodInvoker = commandHandler.lookupInvoker(inputTypeUrl)

    entity._internalSetCurrentState(state)

    methodInvoker
      .invoke(entity, invocationContext)
      .asInstanceOf[EventSourcedEntity.Effect[_]]
  }
}
