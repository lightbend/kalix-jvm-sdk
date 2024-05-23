/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.workflow

import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Message => JavaPbMessage }
import io.grpc.Status
import kalix.protocol.component
import kalix.protocol.entity.Command
import kalix.protocol.workflow_entity.WorkflowStreamIn.{ Message => InMessage }
import kalix.protocol.workflow_entity.WorkflowStreamOut.{ Message => OutMessage }
import kalix.protocol.workflow_entity._
import kalix.protocol.workflow_entity.{ NoTransition => ProtoNoTransition }
import kalix.testkit.entity.EntityMessages
import scalapb.{ GeneratedMessage => ScalaPbMessage }

object WorkflowMessages extends EntityMessages {

  def init(serviceName: String, entityId: String): InMessage =
    init(serviceName, entityId, None)

  def init(serviceName: String, entityId: String, payload: JavaPbMessage): InMessage = {
    // FIXME: workflow payload should not be a snapshot
    // entity in proxy is ES, but its snapshot is not the workflow state
    init(serviceName, entityId, messagePayload(payload))
  }

  def init(serviceName: String, entityId: String, payload: ScalaPbMessage): InMessage =
    init(serviceName, entityId, messagePayload(payload))

  def init(serviceName: String, entityId: String, state: Option[ScalaPbAny]): InMessage =
    InMessage.Init(WorkflowEntityInit(serviceName, entityId, state))

  def command(id: Long, entityId: String, name: String): InMessage =
    command(id, entityId, name, EmptyJavaMessage)

  def command(id: Long, entityId: String, name: String, payload: JavaPbMessage): InMessage =
    command(id, entityId, name, messagePayload(payload))

  def command(id: Long, entityId: String, name: String, payload: ScalaPbMessage): InMessage =
    command(id, entityId, name, messagePayload(payload))

  def command(id: Long, entityId: String, name: String, payload: Option[ScalaPbAny]): InMessage =
    InMessage.Command(Command(entityId, id, name, payload))

  def executeStep(id: Long, stepName: String, input: JavaPbMessage, userState: ScalaPbMessage): InMessage = {
    val executeStep =
      ExecuteStep.defaultInstance
        .withCommandId(id)
        .withStepName(stepName)
        .withInput(protobufAny(input))
        .withUserState(protobufAny(userState))

    InMessage.Step(executeStep)
  }

  def executeStep(id: Long, stepName: String): InMessage = {
    InMessage.Step(ExecuteStep(id, stepName, None, None))
  }

  def executeStep(id: Long, stepName: String, state: ScalaPbAny): InMessage = {
    InMessage.Step(ExecuteStep(id, stepName, None, Some(state)))
  }

  def getNextStep(id: Long, stepName: String, input: JavaPbMessage): InMessage = {
    val nextStep =
      GetNextStep.defaultInstance
        .withCommandId(id)
        .withStepName(stepName)
        .withResult(protobufAny(input))
    InMessage.Transition(nextStep)
  }

  def getNextStep(id: Long, stepName: String, input: ScalaPbAny): InMessage = {
    val nextStep =
      GetNextStep.defaultInstance
        .withCommandId(id)
        .withStepName(stepName)
        .withResult(input)
    InMessage.Transition(nextStep)
  }

  def actionFailure(id: Long, description: String, statusCode: Status.Code): OutMessage = {
    val failure = component.Failure(id, description, statusCode.value())
    val failureClientAction = WorkflowClientAction.defaultInstance.withFailure(failure)
    val noTransition = WorkflowEffect.Transition.NoTransition(ProtoNoTransition.defaultInstance)
    val failureEffect = WorkflowEffect.defaultInstance
      .withClientAction(failureClientAction)
      .withTransition(noTransition)
      .withCommandId(id)
    WorkflowStreamOut.Message.Effect(failureEffect)
  }

  def workflowActionReply(payload: Option[ScalaPbAny]): Option[WorkflowClientAction] = {
    Some(WorkflowClientAction(WorkflowClientAction.Action.Reply(component.Reply(payload, None))))
  }

  def stepTransition(stepName: String): WorkflowEffect.Transition.StepTransition =
    WorkflowEffect.Transition.StepTransition(StepTransition(stepName))

  def reply(id: Long, payload: ScalaPbAny): OutMessage =
    replyAction(id, workflowActionReply(Some(payload)), None, WorkflowEffect.Transition.NoTransition(NoTransition()))

  def reply(id: Long, payload: ScalaPbAny, transition: WorkflowEffect.Transition): OutMessage =
    replyAction(id, workflowActionReply(Some(payload)), None, transition)

  def reply(id: Long, payload: ScalaPbAny, state: ScalaPbAny, transition: WorkflowEffect.Transition): OutMessage =
    replyAction(id, workflowActionReply(Some(payload)), Some(state), transition)

  def replyAction(
      id: Long,
      action: Option[WorkflowClientAction],
      state: Option[ScalaPbAny],
      transition: WorkflowEffect.Transition): OutMessage = {
    OutMessage.Effect(WorkflowEffect(id, action, state, transition))
  }

  def stepExecuted(id: Long, stepName: String, result: ScalaPbAny): OutMessage = {
    OutMessage.Response(StepResponse(id, stepName, StepResponse.Response.Executed(StepExecuted(Some(result)))))
  }

  def stepDeferredCall(
      id: Long,
      stepName: String,
      serviceName: String,
      commandName: String,
      payload: ScalaPbAny): OutMessage = {
    OutMessage.Response(
      StepResponse(
        id,
        stepName,
        StepResponse.Response.DeferredCall(StepDeferredCall(serviceName, commandName, Some(payload), None))))
  }

  def end(id: Long, state: ScalaPbAny): OutMessage = {
    OutMessage.Effect(
      WorkflowEffect(
        id,
        workflowActionReply(None),
        Some(state),
        WorkflowEffect.Transition.EndTransition(EndTransition())))
  }

  def config(): OutMessage =
    WorkflowStreamOut.Message.Config(WorkflowConfig(defaultStepConfig = Some(StepConfig("", None, None))))
}
