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

  final case class PersistenceEffectBuilderImpl[S](
      javasdkEffect: workflow.AbstractWorkflow.Effect.PersistenceEffectBuilder[S])
      extends PersistenceEffectBuilder[S] {

    override def pause(): TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.pause())

    override def transitionTo[I](stepName: String, input: I): TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.transitionTo(stepName, input))

    override def transitionTo(stepName: String): TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.transitionTo(stepName))

    override def end(): TransitionalEffect[Void] =
      TransitionalEffectImpl(javasdkEffect.end())
  }

  final case class ErrorEffectImpl[R](javasdkEffect: workflow.AbstractWorkflow.Effect.ErrorEffect[T])
      extends ErrorEffect[R]

}
