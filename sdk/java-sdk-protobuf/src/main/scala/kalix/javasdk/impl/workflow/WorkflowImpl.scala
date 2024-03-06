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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import com.google.protobuf.duration
import com.google.protobuf.duration.Duration
import io.grpc.Status
import kalix.javasdk.impl.WorkflowExceptions.{ failureMessageForLog, ProtocolException, WorkflowException }
import kalix.javasdk.impl.ErrorHandling.BadRequestException
import kalix.javasdk.impl.timer.TimerSchedulerImpl
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.DeleteState
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.End
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.ErrorEffectImpl
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.NoPersistence
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.NoReply
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.NoTransition
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.Persistence
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.Reply
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.ReplyValue
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.StepTransition
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.TransitionalEffectImpl
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.UpdateState
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.Pause
import kalix.javasdk.impl.workflow.WorkflowRouter.CommandResult
import kalix.javasdk.workflow.CommandContext
import kalix.protocol.component
import kalix.protocol.component.{ Reply => ProtoReply }
import kalix.protocol.workflow_entity.RecoverStrategy
import kalix.protocol.workflow_entity.StepConfig
import kalix.protocol.workflow_entity.WorkflowClientAction
import kalix.protocol.workflow_entity.WorkflowConfig
import kalix.protocol.workflow_entity.WorkflowEffect
import kalix.protocol.workflow_entity.WorkflowEntities
import kalix.protocol.workflow_entity.WorkflowEntityInit
import kalix.protocol.workflow_entity.WorkflowStreamIn
import kalix.protocol.workflow_entity.WorkflowStreamIn.Message.Empty
import kalix.protocol.workflow_entity.WorkflowStreamIn.Message.Init
import kalix.protocol.workflow_entity.WorkflowStreamIn.Message.Step
import kalix.protocol.workflow_entity.WorkflowStreamIn.Message.Transition
import kalix.protocol.workflow_entity.WorkflowStreamIn.Message.{ Command => InCommand }
import kalix.protocol.workflow_entity.WorkflowStreamOut
import kalix.protocol.workflow_entity.WorkflowStreamOut.Message.{ Failure => OutFailure }
import kalix.protocol.workflow_entity.{ EndTransition => ProtoEndTransition }
import kalix.protocol.workflow_entity.{ StepTransition => ProtoStepTransition }
import kalix.protocol.workflow_entity.{ Pause => ProtoPause }
import kalix.protocol.workflow_entity.{ NoTransition => ProtoNoTransition }
import org.slf4j.LoggerFactory
import java.util.Optional

import scala.jdk.OptionConverters._

import kalix.javasdk.workflow.AbstractWorkflow
import kalix.javasdk.workflow.Workflow
import kalix.javasdk.workflow.AbstractWorkflow.WorkflowDef
import kalix.javasdk.workflow.WorkflowContext
import kalix.javasdk.workflow.WorkflowOptions
// FIXME these don't seem to be 'public API', more internals?
import com.google.protobuf.Descriptors
import kalix.javasdk.Metadata
import kalix.javasdk.impl._
import scala.jdk.CollectionConverters._

final class WorkflowService(
    val factory: WorkflowFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    override val additionalDescriptors: Array[Descriptors.FileDescriptor],
    val messageCodec: MessageCodec,
    override val serviceName: String,
    val workflowOptions: Option[WorkflowOptions])
    extends Service {

  def this(
      factory: WorkflowFactory,
      descriptor: Descriptors.ServiceDescriptor,
      additionalDescriptors: Array[Descriptors.FileDescriptor],
      messageCodec: MessageCodec,
      workflowName: String,
      workflowOptions: WorkflowOptions) =
    this(factory, descriptor, additionalDescriptors, messageCodec, workflowName, Some(workflowOptions))

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _                               => None
    }

  override final val componentType = WorkflowEntities.name

  override def componentOptions: Option[ComponentOptions] = workflowOptions
}

final class WorkflowImpl(system: ActorSystem, val services: Map[String, WorkflowService])
    extends kalix.protocol.workflow_entity.WorkflowEntities {

  private implicit val ec: ExecutionContext = system.dispatcher
  private final val log = LoggerFactory.getLogger(this.getClass)

  override def handle(in: Source[WorkflowStreamIn, NotUsed]): Source[WorkflowStreamOut, NotUsed] =
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(WorkflowStreamIn(Init(init), _)), source) =>
          val (flow, config) = runWorkflow(init)
          Source.single(config).concat(source.via(flow))

        case (Seq(), _) =>
          // if error during recovery in proxy the stream will be completed before init
          log.warn("Workflow stream closed before init.")
          Source.empty[WorkflowStreamOut]

        case (Seq(WorkflowStreamIn(other, _)), _) =>
          throw ProtocolException(s"Expected init message for Workflow, but received [${other.getClass.getName}]")
      }
      .recover { case error =>
        ErrorHandling.withCorrelationId { correlationId =>
          log.error(failureMessageForLog(error), error)
          WorkflowStreamOut(OutFailure(component.Failure(description = s"Unexpected error [$correlationId]")))
        }
      }
      .async

  private def toRecoverStrategy(messageCodec: MessageCodec)(
      recoverStrategy: AbstractWorkflow.RecoverStrategy[_]): RecoverStrategy = {
    RecoverStrategy(
      maxRetries = recoverStrategy.maxRetries,
      failoverTo = Some(
        ProtoStepTransition(
          recoverStrategy.failoverStepName,
          recoverStrategy.failoverStepInput.toScala.map(messageCodec.encodeScala))))
  }

  private def toStepConfig(
      name: String,
      timeout: Optional[java.time.Duration],
      recoverStrategy: Option[AbstractWorkflow.RecoverStrategy[_]],
      messageCodec: MessageCodec) = {
    val stepTimeout = timeout.toScala.map(duration.Duration(_))
    val stepRecoverStrategy = recoverStrategy.map(toRecoverStrategy(messageCodec))
    StepConfig(name, stepTimeout, stepRecoverStrategy)
  }

  private def toWorkflowConfig(workflowDefinition: WorkflowDef[_], messageCodec: MessageCodec): WorkflowConfig = {
    val workflowTimeout = workflowDefinition.getWorkflowTimeout.toScala.map(Duration(_))
    val stepConfigs = workflowDefinition.getStepConfigs.asScala
      .map(c => toStepConfig(c.stepName, c.timeout, c.recoverStrategy.toScala, messageCodec))
      .toSeq
    val stepConfig =
      toStepConfig(
        "",
        workflowDefinition.getStepTimeout,
        workflowDefinition.getStepRecoverStrategy.toScala,
        messageCodec)

    val failoverTo = workflowDefinition.getFailoverStepName.toScala.map(stepName => {
      ProtoStepTransition(stepName, workflowDefinition.getFailoverStepInput.toScala.map(messageCodec.encodeScala))
    })

    val failoverRecovery =
      workflowDefinition.getFailoverMaxRetries.toScala.map(strategy => RecoverStrategy(strategy.getMaxRetries))

    WorkflowConfig(workflowTimeout, failoverTo, failoverRecovery, Some(stepConfig), stepConfigs)
  }

  private def runWorkflow(
      init: WorkflowEntityInit): (Flow[WorkflowStreamIn, WorkflowStreamOut, NotUsed], WorkflowStreamOut) = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))
    val router: WorkflowRouter[_, _] =
      service.factory.create(new WorkflowContextImpl(init.entityId, system))
    val workflowId = init.entityId

    val workflowConfig =
      WorkflowStreamOut(
        WorkflowStreamOut.Message.Config(toWorkflowConfig(router._getWorkflowDefinition(), service.messageCodec)))

    init.userState match {
      case Some(state) =>
        val decoded = service.messageCodec.decodeMessage(state)
        router._internalSetInitState(decoded, init.finished)
      case None => // no initial state
    }

    def toProtoEffect(effect: AbstractWorkflow.Effect[_], commandId: Long) = {

      def effectMessage[R](persistence: Persistence[_], transition: WorkflowEffectImpl.Transition, reply: Reply[R]) = {

        val protoEffect =
          persistence match {
            case UpdateState(newState) =>
              router._internalSetInitState(newState, transition.isInstanceOf[End.type])
              WorkflowEffect.defaultInstance.withUserState(service.messageCodec.encodeScala(newState))
            // TODO: persistence should be optional, but we must ensure that we don't save it back to null
            // and preferably we should not even send it over the wire.
            case NoPersistence => WorkflowEffect.defaultInstance
            case DeleteState   => throw new RuntimeException("Workflow state deleted not yet supported")
          }

        val toProtoTransition =
          transition match {
            case StepTransition(stepName, input) =>
              WorkflowEffect.Transition.StepTransition(
                ProtoStepTransition(stepName, input.map(service.messageCodec.encodeScala)))
            case Pause        => WorkflowEffect.Transition.Pause(ProtoPause.defaultInstance)
            case NoTransition => WorkflowEffect.Transition.NoTransition(ProtoNoTransition.defaultInstance)
            case End          => WorkflowEffect.Transition.EndTransition(ProtoEndTransition.defaultInstance)
          }

        val clientAction = {
          val protoReply =
            reply match {
              case ReplyValue(value, metadata) =>
                ProtoReply(
                  payload = Some(service.messageCodec.encodeScala(value)),
                  metadata = MetadataImpl.toProtocol(metadata))
              case NoReply => ProtoReply.defaultInstance
            }
          WorkflowClientAction.defaultInstance.withReply(protoReply)
        }
        protoEffect
          .withTransition(toProtoTransition)
          .withClientAction(clientAction)
      }

      effect match {
        case error: ErrorEffectImpl[_] =>
          val statusCode = error.status.map(_.value()).getOrElse(Status.Code.UNKNOWN.value())
          val failure = component.Failure(commandId, error.description, statusCode)
          val failureClientAction = WorkflowClientAction.defaultInstance.withFailure(failure)
          val noTransition = WorkflowEffect.Transition.NoTransition(ProtoNoTransition.defaultInstance)
          val failureEffect = WorkflowEffect.defaultInstance
            .withClientAction(failureClientAction)
            .withTransition(noTransition)
            .withCommandId(commandId)
          WorkflowStreamOut(WorkflowStreamOut.Message.Effect(failureEffect))

        case WorkflowEffectImpl(persistence, transition, reply) =>
          val protoEffect =
            effectMessage(persistence, transition, reply)
              .withCommandId(commandId)
          WorkflowStreamOut(WorkflowStreamOut.Message.Effect(protoEffect))

        case TransitionalEffectImpl(persistence, transition) =>
          val protoEffect =
            effectMessage(persistence, transition, NoReply)
              .withCommandId(commandId)
          WorkflowStreamOut(WorkflowStreamOut.Message.Effect(protoEffect))
      }
    }

    val flow = Flow[WorkflowStreamIn]
      .map(_.message)
      .mapAsync(1) {

        case InCommand(command) if workflowId != command.entityId =>
          Future.failed(ProtocolException(command, "Receiving Workflow is not the intended recipient of command"))

        case InCommand(command) if command.payload.isEmpty =>
          Future.failed(ProtocolException(command, "No command payload for Workflow"))

        case InCommand(command) =>
          val metadata = MetadataImpl.of(command.metadata.map(_.entries.toVector).getOrElse(Nil))

          val context = new CommandContextImpl(workflowId, command.name, command.id, metadata, system)
          val timerScheduler = new TimerSchedulerImpl(service.messageCodec, system, context.componentCallMetadata)

          val cmd =
            service.messageCodec.decodeMessage(
              command.payload.getOrElse(throw ProtocolException(command, "No command payload")))

          val CommandResult(effect) =
            try {
              router._internalHandleCommand(command.name, cmd, context, timerScheduler)
            } catch {
              case BadRequestException(msg) =>
                CommandResult(WorkflowEffectImpl[Any]().error(msg, Status.Code.INVALID_ARGUMENT))
              case e: WorkflowException => throw e
              case NonFatal(error) =>
                throw WorkflowException(command, s"Unexpected failure: $error", Some(error))
            } finally {
              context.deactivate() // Very important!
            }

          Future.successful(toProtoEffect(effect, command.id))

        case Step(executeStep) =>
          val context =
            new CommandContextImpl(workflowId, executeStep.stepName, executeStep.commandId, Metadata.EMPTY, system)
          val timerScheduler = new TimerSchedulerImpl(service.messageCodec, system, context.componentCallMetadata)
          val stepResponse =
            try {
              executeStep.userState.foreach { state =>
                val decoded = service.messageCodec.decodeMessage(state)
                router._internalSetInitState(decoded, finished = false) // here we know that workflow is still running
              }
              router._internalHandleStep(
                executeStep.commandId,
                executeStep.input,
                executeStep.stepName,
                service.messageCodec,
                timerScheduler,
                context,
                system.dispatcher)
            } catch {
              case e: WorkflowException => throw e
              case NonFatal(ex) =>
                throw WorkflowException(
                  s"unexpected exception [${ex.getMessage}] while executing step [${executeStep.stepName}]",
                  Some(ex))
            }

          stepResponse.map { stp =>
            WorkflowStreamOut(WorkflowStreamOut.Message.Response(stp))
          }

        case Transition(cmd) =>
          val CommandResult(effect) =
            try {
              router._internalGetNextStep(cmd.stepName, cmd.result.get, service.messageCodec)
            } catch {
              case e: WorkflowException => throw e
              case NonFatal(ex) =>
                throw WorkflowException(
                  s"unexpected exception [${ex.getMessage}] while executing transition for step [${cmd.stepName}]",
                  Some(ex))
            }

          Future.successful(toProtoEffect(effect, cmd.commandId))

        case Init(_) =>
          throw ProtocolException(init, "Workflow already initiated")

        case Empty =>
          throw ProtocolException(init, "Workflow received empty/unknown message")
      }

    (flow, workflowConfig)
  }

}

private[kalix] final class CommandContextImpl(
    override val workflowId: String,
    override val commandName: String,
    override val commandId: Long,
    override val metadata: Metadata,
    system: ActorSystem)
    extends AbstractContext(system)
    with CommandContext
    with ActivatableContext

private[kalix] final class WorkflowContextImpl(override val workflowId: String, system: ActorSystem)
    extends AbstractContext(system)
    with WorkflowContext
