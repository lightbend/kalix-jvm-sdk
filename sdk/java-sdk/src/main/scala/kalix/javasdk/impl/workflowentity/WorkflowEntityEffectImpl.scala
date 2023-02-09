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

package kalix.javasdk.impl.workflowentity

import io.grpc.Status
import kalix.javasdk.Metadata
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.End
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.ErrorEffectImpl
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.NoPersistence
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.NoTransition
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.Persistence
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.PersistenceEffectBuilderImpl
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.Reply
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.StepTransition
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.Transition
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.TransitionalEffectImpl
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.UpdateState
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl.Pause
import kalix.javasdk.workflowentity.WorkflowEntity.Effect
import kalix.javasdk.workflowentity.WorkflowEntity.Effect.Builder
import kalix.javasdk.workflowentity.WorkflowEntity.Effect.PersistenceEffectBuilder
import kalix.javasdk.workflowentity.WorkflowEntity.Effect.TransitionalEffect

object WorkflowEntityEffectImpl {

  sealed trait Transition
  case class StepTransition[I](input: I, transitionTo: String) extends Transition
  object Pause extends Transition
  object NoTransition extends Transition
  object End extends Transition

  sealed trait Persistence[+S]
  final case class UpdateState[S](newState: S) extends Persistence[S]
  case object DeleteState extends Persistence[Nothing]
  case object NoPersistence extends Persistence[Nothing]

  sealed trait Reply[+R]
  case class ReplyValue[R](value: R, metadata: Metadata) extends Reply[R]
  case object NoReply extends Reply[Nothing]

  def apply[S](): WorkflowEntityEffectImpl[S, S] = WorkflowEntityEffectImpl(NoPersistence, Pause, NoReply)

  final case class PersistenceEffectBuilderImpl[S](persistence: Persistence[S]) extends PersistenceEffectBuilder[S] {

    override def pause(): TransitionalEffect[Void] =
      TransitionalEffectImpl(persistence, Pause)

    override def transitionTo[I](transitionTo: String, input: I): TransitionalEffect[Void] =
      TransitionalEffectImpl(persistence, StepTransition(input, transitionTo))

    override def end(): TransitionalEffect[Void] =
      TransitionalEffectImpl(persistence, End)

  }

  final case class TransitionalEffectImpl[S, T](persistence: Persistence[S], transition: Transition)
      extends TransitionalEffect[T] {

    override def thenReply[R](message: R): Effect[R] =
      WorkflowEntityEffectImpl(persistence, transition, ReplyValue(message, Metadata.EMPTY))

    override def thenReply[R](message: R, metadata: Metadata): Effect[R] =
      WorkflowEntityEffectImpl(persistence, transition, ReplyValue(message, metadata))
  }

  final case class ErrorEffectImpl[R](description: String, status: Option[Status.Code]) extends Effect.ErrorEffect[R]
}
case class WorkflowEntityEffectImpl[S, T](persistence: Persistence[S], transition: Transition, reply: Reply[T])
    extends Builder[S]
    with Effect[T] {

  override def updateState(newState: S): PersistenceEffectBuilder[S] =
    PersistenceEffectBuilderImpl(UpdateState(newState))

  override def pause(): TransitionalEffect[Void] =
    TransitionalEffectImpl(NoPersistence, Pause)

  override def transitionTo[I](transitionTo: String, input: I): TransitionalEffect[Void] =
    TransitionalEffectImpl(NoPersistence, StepTransition(input, transitionTo))

  override def end(): TransitionalEffect[Void] =
    TransitionalEffectImpl(NoPersistence, End)

  override def reply[R](reply: R): Effect[R] =
    TransitionalEffectImpl(NoPersistence, NoTransition).thenReply(reply)

  override def reply[R](reply: R, metadata: Metadata): Effect[R] =
    TransitionalEffectImpl(NoPersistence, NoTransition).thenReply(reply, metadata)

  override def error[R](description: String): Effect.ErrorEffect[R] =
    ErrorEffectImpl(description, None)

  override def error[R](description: String, statusCode: Status.Code): Effect.ErrorEffect[R] =
    ErrorEffectImpl(description, Option(statusCode))

}
