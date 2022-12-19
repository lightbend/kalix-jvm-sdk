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

package kalix.javasdk.impl.workflow

import java.util.Optional

import kalix.javasdk.impl.workflow.WorkflowRouter.CommandHandlerNotFound
import kalix.javasdk.impl.workflow.WorkflowRouter.CommandResult
import kalix.javasdk.workflow.CommandContext
import kalix.javasdk.workflow.Workflow
import kalix.javasdk.workflow.Workflow.Effect

object WorkflowRouter {
  final case class CommandResult(effect: Workflow.Effect[_])

  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException
}

abstract class WorkflowRouter[S, W <: Workflow[S]](protected val workflow: W) {

  private var state: Option[S] = None

  private def stateOrEmpty(): S = state match {
    case None =>
      val emptyState = workflow.emptyState()
      // null is allowed as emptyState
      state = Some(emptyState)
      emptyState
    case Some(state) =>
      state
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  def _internalSetInitState(s: Any): Unit = {
    state = Some(s.asInstanceOf[S])
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHandleCommand(commandName: String, command: Any, context: CommandContext): CommandResult = {
    val commandEffect =
      try {
        workflow._internalSetCommandContext(Optional.of(context))
        workflow._internalSetCurrentState(stateOrEmpty())
        handleCommand(commandName, stateOrEmpty(), command, context).asInstanceOf[Effect[Any]]
      } catch {
        case CommandHandlerNotFound(name) =>
          throw new WorkflowExceptions.WorkflowException(
            context.workflowId(),
            context.commandId(),
            commandName,
            s"No command handler found for command [$name] on ${workflow.getClass}")
      } finally {
        workflow._internalSetCommandContext(Optional.empty())
      }

//    if (!commandEffect.hasError()) {
//      commandEffect.primaryEffect match {
//        case UpdateState(newState) =>
//          if (newState == null)
//            throw new IllegalArgumentException("updateState with null state is not allowed.")
//          state = Some(newState.asInstanceOf[S])
//        case DeleteState => state = None
//        case _           =>
//      }
//    }

    CommandResult(commandEffect)
  }

  protected def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): Workflow.Effect[_]

//  // def onInput(state: S, input: Any, commandName: String)
//
//  def onInput(state: S, input: Any, stepName: Option[String]): S = {
//
//    val workflowDef = workflow.definition()
//    val step =
//      stepName
//        .flatMap { name => workflowDef.findByName(name).toScala }
//        .getOrElse(???)
//
//    step match {
//      case call: Call[_, _, _] =>
//        val defCall =
//          call.callFunc
//            .asInstanceOf[Function[Any, DeferredCall[Any, Any]]]
//            .apply(input)
//        ???
//      case _ => ???
//    }
//
//  }
//
//  def onSuccess(state: S, result: Any, executedStepName: String): S = {
//    val workflowDef = workflow.definition()
//    // TODO: if empty means we executed something that was removed
//    // need to fail workflow with enough context to users
//    val step = workflowDef.findByName(executedStepName).get()
//
//    step match {
//      case action: Call[_, _, _] =>
//        val transformedResult =
//          action.transitionFunc
//            .asInstanceOf[JFunc[Any, Any]]
//            .apply(result)
//        ???
//      case _ => ???
//    }
//  }

}
