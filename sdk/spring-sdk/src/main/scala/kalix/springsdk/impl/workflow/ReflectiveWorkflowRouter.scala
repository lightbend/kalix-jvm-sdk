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

package kalix.springsdk.impl.workflow

import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.workflow.WorkflowRouter
import kalix.javasdk.workflow.CommandContext
import kalix.javasdk.workflow.Workflow
import kalix.springsdk.impl.CommandHandler
import kalix.springsdk.impl.InvocationContext

import java.lang.reflect.ParameterizedType

class ReflectiveWorkflowRouter[S, W <: Workflow[S]](
    override protected val workflow: W,
    commandHandlers: Map[String, CommandHandler])
    extends WorkflowRouter[S, W](workflow) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(
      commandName,
      throw new HandlerNotFoundException("command", commandName, commandHandlers.keySet))

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      commandContext: CommandContext): Workflow.Effect[_] = {

    _extractAndSetCurrentState(state)

    val commandHandler = commandHandlerLookup(commandName)
    val invocationContext =
      InvocationContext(
        command.asInstanceOf[ScalaPbAny],
        commandHandler.requestMessageDescriptor,
        commandContext.metadata())

    val inputTypeUrl = command.asInstanceOf[ScalaPbAny].typeUrl

    commandHandler
      .getInvoker(inputTypeUrl)
      .invoke(workflow, invocationContext)
      .asInstanceOf[Workflow.Effect[_]]
  }

  private def _extractAndSetCurrentState(state: S): Unit = {
    val workflowStateType: Class[S] =
      this.workflow.getClass.getGenericSuperclass
        .asInstanceOf[ParameterizedType]
        .getActualTypeArguments
        .head
        .asInstanceOf[Class[S]]

    // the state: S received can either be of the entity "state" type (if coming from emptyState/memory)
    // or PB Any type (if coming from the proxy)
    state match {
      case s if s == null || state.getClass == workflowStateType =>
        // note that we set the state even if null, this is needed in order to
        // be able to call currentState() later
        workflow._internalSetCurrentState(s)
      case s =>
        val deserializedState =
          JsonSupport.decodeJson(workflowStateType, ScalaPbAny.toJavaProto(s.asInstanceOf[ScalaPbAny]))
        workflow._internalSetCurrentState(deserializedState)
    }
  }
}

final class HandlerNotFoundException(handlerType: String, name: String, availableHandlers: Set[String])
    extends RuntimeException(
      s"no matching $handlerType handler for '$name'. " +
      s"Available handlers are: [${availableHandlers.mkString(", ")}]")
