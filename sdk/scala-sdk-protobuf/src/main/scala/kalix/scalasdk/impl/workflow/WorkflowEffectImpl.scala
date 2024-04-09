/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.workflow

import io.grpc.Status
import kalix.javasdk
import kalix.scalasdk.Metadata
import kalix.javasdk.workflow
import kalix.scalasdk.impl.workflow.WorkflowEffectImpl.TransitionalEffectImpl
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.AbstractWorkflow.Effect
import kalix.scalasdk.workflow.AbstractWorkflow.Effect.ErrorEffect
import kalix.scalasdk.workflow.AbstractWorkflow.Effect.PersistenceEffectBuilder
import kalix.scalasdk.workflow.AbstractWorkflow.Effect.TransitionalEffect

private[scalasdk] object WorkflowEffectImpl {
  def apply[S](): WorkflowEffectImpl[S, S] = WorkflowEffectImpl(javasdk.impl.workflow.WorkflowEffectImpl())

  final case class TransitionalEffectImpl[T](javasdkEffect: workflow.AbstractWorkflow.Effect.TransitionalEffect[T])
      extends TransitionalEffect[T] {

    override def thenReply[R](message: R): Effect[R] = {
      WorkflowEffectImpl(javasdkEffect.thenReply(message).asInstanceOf[javasdk.impl.workflow.WorkflowEffectImpl[R, T]])
    }

    override def thenReply[R](message: R, metadata: Metadata): Effect[R] =
      WorkflowEffectImpl(
        javasdkEffect.thenReply(message, metadata.impl).asInstanceOf[javasdk.impl.workflow.WorkflowEffectImpl[R, T]])
  }
}

private[scalasdk] final case class WorkflowEffectImpl[S, T](
    javasdkEffect: javasdk.impl.workflow.WorkflowEffectImpl[S, T])
    extends AbstractWorkflow.Effect.Builder[S]
//    with AbstractWorkflow.Effect.ErrorEffect[S]
    with AbstractWorkflow.Effect[S] {

  override def updateState(newState: S): PersistenceEffectBuilder[S] =
    PersistenceEffectBuilderImpl(javasdkEffect.updateState(newState))

  override def pause: TransitionalEffect[Void] =
    TransitionalEffectImpl(javasdkEffect.pause())

  override def transitionTo[I](stepName: String, input: I): TransitionalEffect[Void] =
    TransitionalEffectImpl(javasdkEffect.transitionTo(stepName, input))

  override def transitionTo(stepName: String): TransitionalEffect[Void] =
    TransitionalEffectImpl(javasdkEffect.transitionTo(stepName))

  override def end: TransitionalEffect[Void] =
    TransitionalEffectImpl(javasdkEffect.end())

  override def reply[R](replyMessage: R): AbstractWorkflow.Effect[R] =
    WorkflowEffectImpl(javasdkEffect.reply(replyMessage).asInstanceOf[javasdk.impl.workflow.WorkflowEffectImpl[R, T]])

  override def reply[R](message: R, metadata: Metadata): AbstractWorkflow.Effect[R] =
    WorkflowEffectImpl(
      javasdkEffect.reply(message, metadata.impl).asInstanceOf[javasdk.impl.workflow.WorkflowEffectImpl[R, T]])

  override def error[R](description: String): ErrorEffect[R] = ErrorEffectImpl(javasdkEffect.error(description))

  override def error[R](description: String, statusCode: Status.Code): ErrorEffect[R] = ErrorEffectImpl(
    javasdkEffect.error(description, statusCode))

  case class PersistenceEffectBuilderImpl(javasdkEffect: workflow.AbstractWorkflow.Effect.PersistenceEffectBuilder[S])
      extends PersistenceEffectBuilder[S] {

    override def pause: TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.pause())

    override def transitionTo[I](stepName: String, input: I): TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.transitionTo(stepName, input))

    override def transitionTo(stepName: String): TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.transitionTo(stepName))

    override def end: TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.end())
  }

  case class ErrorEffectImpl[R](javasdkEffect: workflow.AbstractWorkflow.Effect.ErrorEffect[T]) extends ErrorEffect[R]

}
