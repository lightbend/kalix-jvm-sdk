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
import kalix.springsdk.impl.{ ComponentMethod, InvocationContext }

import java.util.Optional

class ReflectiveEventSourcedEntityRouter[S, E <: EventSourcedEntity[S]](
    override protected val entity: E,
    componentMethods: Map[String, ComponentMethod])
    extends EventSourcedEntityRouter[S, E](entity) {

  private def methodLookup(commandName: String) =
    componentMethods.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleEvent(state: S, event: Any): S = {
    //TODO match with handler for event
    state
  }

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: CommandContext): EventSourcedEntity.Effect[_] = {

    val componentMethod = methodLookup(commandName)
    val invocationContext =
      InvocationContext(command.asInstanceOf[ScalaPbAny], componentMethod.requestMessageDescriptor)

    // pass current state to entity
    entity._internalSetCommandContext(Optional.of(context))

    // safe call: if component method is None, proxy won't forward calls to it
    // typically, that happens when we have a View update method with transform = false
    // in such a case, the proxy can index the view payload directly, without passing through the user function
    componentMethod.method.get
      .invoke(entity, componentMethod.parameterExtractors.map(e => e.extract(invocationContext)): _*)
      .asInstanceOf[EventSourcedEntity.Effect[_]]
  }
}
