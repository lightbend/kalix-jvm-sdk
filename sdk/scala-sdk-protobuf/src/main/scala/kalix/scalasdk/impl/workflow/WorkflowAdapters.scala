/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.workflow

import java.util.Optional

import scala.compat.java8.DurationConverters.FiniteDurationops
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.jdk.FutureConverters.FutureOps
import scala.jdk.OptionConverters._

import akka.stream.Materializer
import com.google.protobuf.Descriptors
import kalix.javasdk
import kalix.javasdk.impl
import kalix.javasdk.timer.TimerScheduler
import kalix.scalasdk.impl.InternalContext
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.impl.MetadataImpl
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.timer.TimerSchedulerImpl
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.AbstractWorkflow.AsyncCallStep
import kalix.scalasdk.workflow.AbstractWorkflow.CallStep
import kalix.scalasdk.workflow.AbstractWorkflow.RecoverStrategy
import kalix.scalasdk.workflow.CommandContext
import kalix.scalasdk.workflow.WorkflowContext
import kalix.scalasdk.workflow.WorkflowOptions
import kalix.scalasdk.workflow.WorkflowProvider

private[scalasdk] final class JavaWorkflowAdapter[S >: Null](scalaSdkWorkflow: AbstractWorkflow[S])
    extends javasdk.workflow.Workflow[S] {

  override def emptyState(): S = scalaSdkWorkflow.emptyState

  override def _internalSetCommandContext(context: Optional[javasdk.workflow.CommandContext]): Unit =
    scalaSdkWorkflow._internalSetCommandContext(context.map(new ScalaCommandContextAdapter(_)).toScala)

  override def _internalSetCurrentState(state: S): Unit = {
    scalaSdkWorkflow._internalSetCurrentState(state)
  }

  override def _internalSetTimerScheduler(timerScheduler: Optional[TimerScheduler]): Unit = {
    scalaSdkWorkflow._internalSetTimerScheduler(timerScheduler.toScala.map {
      case javaTimerScheduler: kalix.javasdk.impl.timer.TimerSchedulerImpl =>
        new TimerSchedulerImpl(
          javaTimerScheduler.messageCodec,
          javaTimerScheduler.system,
          MetadataImpl(javaTimerScheduler.metadata.asInstanceOf[impl.MetadataImpl]))
    })
  }

  override def definition(): javasdk.workflow.AbstractWorkflow.WorkflowDef[S] = {

    def convertToJava(
        recoverStrategy: RecoverStrategy[_]): kalix.javasdk.workflow.AbstractWorkflow.RecoverStrategy[_] = {
      new kalix.javasdk.workflow.AbstractWorkflow.RecoverStrategy(
        recoverStrategy.maxRetries,
        recoverStrategy.failoverStepName,
        recoverStrategy.failoverStepInput.toJava)
    }

    val javaWorkflowDef = workflow()
    val scalaDefinition = scalaSdkWorkflow.definition
    scalaDefinition.steps.map {
      case callStep: CallStep[Any @unchecked, Any @unchecked, Any @unchecked, Any @unchecked] =>
        val javaCallStep = new javasdk.workflow.AbstractWorkflow.CallStep(
          callStep.name,
          callStep.callInputClass,
          (any: Any) => {
            callStep.callFunc(any) match {
              case ScalaDeferredCallAdapter(javaSdkDeferredCall) => javaSdkDeferredCall
            }
          },
          callStep.transitionInputClass,
          (any: Any) => {
            callStep.transitionFunc(any) match {
              case kalix.scalasdk.impl.workflow.WorkflowEffectImpl.TransitionalEffectImpl(javaEffect) => javaEffect
            }
          })
        javaWorkflowDef.addStep(javaCallStep)
      case asyncCallStep: AsyncCallStep[Any @unchecked, Any @unchecked, Any @unchecked] =>
        val javaAsyncCallStep = new javasdk.workflow.AbstractWorkflow.AsyncCallStep(
          asyncCallStep.name,
          asyncCallStep.callInputClass,
          (any: Any) => {
            asyncCallStep.callFunc(any).asJava
          },
          asyncCallStep.transitionInputClass,
          (any: Any) => {
            asyncCallStep.transitionFunc(any) match {
              case kalix.scalasdk.impl.workflow.WorkflowEffectImpl.TransitionalEffectImpl(javaEffect) => javaEffect
            }
          })
        javaWorkflowDef.addStep(javaAsyncCallStep)
    }
    scalaDefinition.workflowTimeout.map(_.toJava).foreach(javaWorkflowDef.timeout)
    scalaDefinition.stepTimeout.map(_.toJava).foreach(javaWorkflowDef.defaultStepTimeout)
    scalaDefinition.stepRecoverStrategy
      .map(convertToJava)
      .foreach(javaWorkflowDef.defaultStepRecoverStrategy)

    scalaDefinition.failoverStepInput match {
      case Some(value) =>
        //when input exists, failoverStepInput and maxRetries must exist
        val javaMaxRetries = scalaDefinition.failoverMaxRetries
          .map(_.maxRetries)
          .map(javasdk.workflow.AbstractWorkflow.RecoverStrategy.maxRetries)
        javaWorkflowDef.failoverTo(scalaDefinition.failoverStepName.get, value, javaMaxRetries.get)
      case None =>
        //when failoverStepInput exists, maxRetries must exist
        val javaMaxRetries = scalaDefinition.failoverMaxRetries
          .map(_.maxRetries)
          .map(javasdk.workflow.AbstractWorkflow.RecoverStrategy.maxRetries)
        scalaDefinition.failoverStepName.map(failoverStepName =>
          javaWorkflowDef.failoverTo(failoverStepName, javaMaxRetries.get))
    }

    javaWorkflowDef
  }
}

private[scalasdk] final class JavaWorkflowProviderAdapter[S >: Null, E <: AbstractWorkflow[S]](
    scalaSdkProvider: WorkflowProvider[S, E])
    extends javasdk.workflow.WorkflowProvider[S, javasdk.workflow.AbstractWorkflow[S]] {

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray

  override def typeId(): String = scalaSdkProvider.typeId

  override def newRouter(context: javasdk.workflow.WorkflowContext)
      : javasdk.impl.workflow.WorkflowRouter[S, javasdk.workflow.AbstractWorkflow[S]] = {

    val scalaSdkRouter = scalaSdkProvider
      .newRouter(new ScalaWorkflowContextAdapter(context))
      .asInstanceOf[WorkflowRouter[S, AbstractWorkflow[S]]]

    new JavaWorkflowRouterAdapter[S](new JavaWorkflowAdapter[S](scalaSdkRouter.workflow), scalaSdkRouter)
  }

  override def options(): javasdk.workflow.WorkflowOptions = new JavaWorkflowOptionsAdapter(scalaSdkProvider.options)

  override def serviceDescriptor(): Descriptors.ServiceDescriptor = scalaSdkProvider.serviceDescriptor
}

private[scalasdk] final class JavaWorkflowRouterAdapter[S >: Null](
    javaSdkWorkflow: javasdk.workflow.Workflow[S],
    scalaSdkRouter: WorkflowRouter[S, AbstractWorkflow[S]])
    extends javasdk.impl.workflow.WorkflowRouter[S, javasdk.workflow.AbstractWorkflow[S]](javaSdkWorkflow) {

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: javasdk.workflow.CommandContext): javasdk.workflow.AbstractWorkflow.Effect[_] = {
    scalaSdkRouter.handleCommand(commandName, state, command, new ScalaCommandContextAdapter(context)) match {
      case WorkflowEffectImpl(javaSdkEffectImpl) => javaSdkEffectImpl
    }
  }
}

private[scalasdk] final class JavaWorkflowOptionsAdapter(scalaSdkWorkflowOptions: WorkflowOptions)
    extends javasdk.workflow.WorkflowOptions {

  def forwardHeaders: java.util.Set[String] = scalaSdkWorkflowOptions.forwardHeaders.asJava

  def withForwardHeaders(headers: java.util.Set[String]): javasdk.workflow.WorkflowOptions =
    new JavaWorkflowOptionsAdapter(scalaSdkWorkflowOptions.withForwardHeaders(Set.from(headers.asScala)))
}

private[scalasdk] final class ScalaCommandContextAdapter(val javaSdkContext: javasdk.workflow.CommandContext)
    extends CommandContext
    with InternalContext {

  override def commandName: String = javaSdkContext.commandName()

  override def commandId: Long = javaSdkContext.commandId()

  override def metadata: kalix.scalasdk.Metadata =
    MetadataConverters.toScala(javaSdkContext.metadata())

  def getComponentGrpcClient[T](serviceClass: Class[T]): T = javaSdkContext match {
    case ctx: javasdk.impl.AbstractContext => ctx.getComponentGrpcClient(serviceClass)
  }

  override def materializer(): Materializer = javaSdkContext.materializer()

  override def workflowId: String = javaSdkContext.workflowId()
}

private[scalasdk] final class ScalaWorkflowContextAdapter(javaSdkContext: javasdk.workflow.WorkflowContext)
    extends WorkflowContext {

  override def materializer(): Materializer = javaSdkContext.materializer()

  override def workflowId: String = javaSdkContext.workflowId()
}
