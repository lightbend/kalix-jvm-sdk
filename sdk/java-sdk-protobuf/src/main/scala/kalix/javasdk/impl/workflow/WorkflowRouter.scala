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

package kalix.javasdk.impl.workflow

import java.nio.ByteBuffer
import java.util.Optional
import java.util.concurrent.CompletionStage
import java.util.function.{ Function => JFunc }

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

import com.google.api.HttpBody
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.DeferredCall
import kalix.javasdk.HttpResponse
import kalix.javasdk.HttpResponse.STATUS_CODE_EXTENSION_TYPE_URL
import kalix.javasdk.JsonSupport
import kalix.javasdk.StatusCode
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.RestDeferredCall
import kalix.javasdk.impl.WorkflowExceptions.WorkflowException
import kalix.javasdk.impl.workflow.WorkflowRouter.CommandHandlerNotFound
import kalix.javasdk.impl.workflow.WorkflowRouter.CommandResult
import kalix.javasdk.impl.workflow.WorkflowRouter.WorkflowStepNotFound
import kalix.javasdk.impl.workflow.WorkflowRouter.WorkflowStepNotSupported
import kalix.javasdk.timer.TimerScheduler
import kalix.javasdk.workflow.AbstractWorkflow
import kalix.javasdk.workflow.AbstractWorkflow.AsyncCallStep
import kalix.javasdk.workflow.AbstractWorkflow.CallStep
import kalix.javasdk.workflow.AbstractWorkflow.Effect
import kalix.javasdk.workflow.AbstractWorkflow.WorkflowDef
import kalix.javasdk.workflow.CommandContext
import kalix.protocol.workflow_entity.StepDeferredCall
import kalix.protocol.workflow_entity.StepExecuted
import kalix.protocol.workflow_entity.StepExecutionFailed
import kalix.protocol.workflow_entity.StepResponse
import org.slf4j.LoggerFactory

object WorkflowRouter {
  final case class CommandResult(effect: AbstractWorkflow.Effect[_])

  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException {
    override def getMessage: String = commandName
  }
  final case class WorkflowStepNotFound(stepName: String) extends RuntimeException {
    override def getMessage: String = stepName
  }

  final case class WorkflowStepNotSupported(stepName: String) extends RuntimeException {
    override def getMessage: String = stepName
  }
}

abstract class WorkflowRouter[S, W <: AbstractWorkflow[S]](protected val workflow: W) {

  private var state: Option[S] = None
  private var workflowFinished: Boolean = false
  private final val log = LoggerFactory.getLogger(this.getClass)

  private def stateOrEmpty(): S = state match {
    case None =>
      val emptyState = workflow.emptyState()
      // null is allowed as emptyState
      state = Some(emptyState)
      emptyState
    case Some(state) =>
      state
  }

  def _getWorkflowDefinition(): WorkflowDef[S] = {
    workflow.definition()
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  def _internalSetInitState(s: Any, finished: Boolean): Unit = {
    if (!workflowFinished) {
      state = Some(s.asInstanceOf[S])
      workflowFinished = finished
    }
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHandleCommand(
      commandName: String,
      command: Any,
      context: CommandContext,
      timerScheduler: TimerScheduler): CommandResult = {
    val commandEffect =
      try {
        workflow._internalSetTimerScheduler(Optional.of(timerScheduler))
        workflow._internalSetCommandContext(Optional.of(context))
        workflow._internalSetCurrentState(stateOrEmpty())
        handleCommand(commandName, stateOrEmpty(), command, context).asInstanceOf[Effect[Any]]
      } catch {
        case CommandHandlerNotFound(name) =>
          throw new WorkflowException(
            context.workflowId(),
            context.commandId(),
            commandName,
            s"No command handler found for command [$name] on ${workflow.getClass}")
      } finally {
        workflow._internalSetCommandContext(Optional.empty())
      }

    CommandResult(commandEffect)
  }

  protected def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: CommandContext): AbstractWorkflow.Effect[_]

  // in same cases, the Proxy may send a message with typeUrl set to object.
  // if that's the case, we need to patch the message using the typeUrl from the expected input class
  private def decodeInput(messageCodec: MessageCodec, result: ScalaPbAny, expectedInputClass: Class[_]) = {
    if (result.typeUrl == JsonSupport.KALIX_JSON + "object") {
      val typeUrl = messageCodec.typeUrlFor(expectedInputClass)
      messageCodec.decodeMessage(result.copy(typeUrl = typeUrl))
    } else if (result.typeUrl == "type.googleapis.com/google.api.HttpBody") {
      val httpBody = HttpBody.parseFrom(result.value.newCodedInput())
      //HttpBodyStatusCodeExtensionTypeUrl constant from java-sdk-spring
      httpBody.getExtensionsList.asScala.find(_.getTypeUrl == STATUS_CODE_EXTENSION_TYPE_URL) match {
        case Some(statusCodeAny) =>
          val statusCode = ByteBuffer.wrap(statusCodeAny.getValue.toByteArray).getInt
          val contentType = httpBody.getContentType
          HttpResponse.of(StatusCode.Success.from(statusCode), contentType, httpBody.getData.toByteArray)
        case None => throw new IllegalStateException("Missing status code extension in HttpBody")
      }
    } else {
      messageCodec.decodeMessage(result)
    }
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHandleStep(
      commandId: Long,
      input: Option[ScalaPbAny],
      stepName: String,
      messageCodec: MessageCodec,
      timerScheduler: TimerScheduler,
      commandContext: CommandContext,
      executionContext: ExecutionContext): Future[StepResponse] = {

    implicit val ec = executionContext

    workflow._internalSetCurrentState(stateOrEmpty())
    workflow._internalSetTimerScheduler(Optional.of(timerScheduler))
    workflow._internalSetCommandContext(Optional.of(commandContext))
    val workflowDef = workflow.definition()

    workflowDef.findByName(stepName).toScala match {
      case Some(call: CallStep[_, _, _, _]) =>
        val decodedInput = input match {
          case Some(inputValue) => decodeInput(messageCodec, inputValue, call.callInputClass)
          case None             => null // to meet a signature of supplier expressed as a function
        }

        val defCall = call.callFunc
          .asInstanceOf[JFunc[Any, DeferredCall[Any, Any]]]
          .apply(decodedInput)

        val (commandName, serviceName) =
          defCall match {
            case grpcDefCall: GrpcDeferredCall[_, _] =>
              (grpcDefCall.methodName, grpcDefCall.fullServiceName)
            case restDefCall: RestDeferredCall[_, _] =>
              (restDefCall.methodName, restDefCall.fullServiceName)
          }

        val stepDefCall =
          StepDeferredCall(
            serviceName,
            commandName,
            payload = Some(messageCodec.encodeScala(defCall.message())),
            metadata = MetadataImpl.toProtocol(defCall.metadata()))

        Future.successful {
          StepResponse(commandId, stepName, StepResponse.Response.DeferredCall(stepDefCall))
        }

      case Some(call: AsyncCallStep[_, _, _]) =>
        val decodedInput = input match {
          case Some(inputValue) => decodeInput(messageCodec, inputValue, call.callInputClass)
          case None             => null // to meet a signature of supplier expressed as a function
        }

        val future = call.callFunc
          .asInstanceOf[JFunc[Any, CompletionStage[Any]]]
          .apply(decodedInput)
          .toScala

        future
          .map { res =>
            val encoded = messageCodec.encodeScala(res)
            val executedRes = StepExecuted(Some(encoded))

            StepResponse(commandId, stepName, StepResponse.Response.Executed(executedRes))
          }
          .recover { case t: Throwable =>
            log.error("Workflow async call failed.", t)
            StepResponse(commandId, stepName, StepResponse.Response.ExecutionFailed(StepExecutionFailed(t.getMessage)))
          }
      case Some(any) => Future.failed(WorkflowStepNotSupported(any.getClass.getSimpleName))
      case None      => Future.failed(WorkflowStepNotFound(stepName))
    }

  }

  def _internalGetNextStep(stepName: String, result: ScalaPbAny, messageCodec: MessageCodec): CommandResult = {

    workflow._internalSetCurrentState(stateOrEmpty())
    val workflowDef = workflow.definition()

    workflowDef.findByName(stepName).toScala match {
      case Some(call: CallStep[_, _, _, _]) =>
        val effect =
          call.transitionFunc
            .asInstanceOf[JFunc[Any, Effect[Any]]]
            .apply(decodeInput(messageCodec, result, call.transitionInputClass))

        CommandResult(effect)

      case Some(call: AsyncCallStep[_, _, _]) =>
        val effect =
          call.transitionFunc
            .asInstanceOf[JFunc[Any, Effect[Any]]]
            .apply(decodeInput(messageCodec, result, call.transitionInputClass))

        CommandResult(effect)

      case Some(any) => throw WorkflowStepNotSupported(any.getClass.getSimpleName)
      case None      => throw WorkflowStepNotFound(stepName)
    }
  }
}
