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

import akka.Done
import io.grpc.Status
import kalix.javasdk.Metadata
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.End
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.ErrorEffectImpl
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.NoPersistence
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.Persistence
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.Reply
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.TransitionalEffectImpl
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.StepTransition
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.Transition
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.PersistenceEffectImpl
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.UpdateState
import kalix.javasdk.impl.workflow.WorkflowEffectImpl.Wait
import kalix.javasdk.workflow.Workflow.Effect
import kalix.javasdk.workflow.Workflow.Effect.Builder
import kalix.javasdk.workflow.Workflow.Effect.PersistenceEffect
import kalix.javasdk.workflow.Workflow.Effect.TransitionalEffect

object WorkflowEffectImpl {

  sealed trait Transition
  case class StepTransition[I](input: I, transitionTo: String) extends Transition
  object Wait extends Transition
  object End extends Transition

  sealed trait Persistence[+S]
  final case class UpdateState[S](newState: S) extends Persistence[S]
  case object DeleteState extends Persistence[Nothing]
  case object NoPersistence extends Persistence[Nothing]

  sealed trait Reply[+R]
  case class ReplyValue[R](reply: R, metadata: Metadata) extends Reply[R]
  case object NoReply extends Reply[Nothing]

  def apply[S](): WorkflowEffectImpl[S, S] = WorkflowEffectImpl(NoPersistence, Wait, NoReply)

  final case class PersistenceEffectImpl[S](persistence: Persistence[S]) extends PersistenceEffect[S] {

    override def waitForInput(): TransitionalEffect[Done] =
      TransitionalEffectImpl(persistence, Wait)

    override def transition[I](input: I, transitionTo: String): TransitionalEffect[Done] =
      TransitionalEffectImpl(persistence, StepTransition(input, transitionTo))

    override def end(): TransitionalEffect[Done] =
      TransitionalEffectImpl(persistence, End)

  }

  final case class TransitionalEffectImpl[S, T](persistence: Persistence[S], transition: Transition)
      extends TransitionalEffect[T] {

    override def thenReply[R](message: R): Effect[R] =
      WorkflowEffectImpl(persistence, transition, ReplyValue(message, Metadata.EMPTY))

    override def thenReply[R](message: R, metadata: Metadata): Effect[R] =
      WorkflowEffectImpl(persistence, transition, ReplyValue(message, metadata))
  }

  final case class ErrorEffectImpl[R](description: String, status: Option[Status.Code]) extends Effect.ErrorEffect[R]
}
case class WorkflowEffectImpl[S, T](persistence: Persistence[S], transition: Transition, reply: Reply[T])
    extends Builder[S]
    with Effect[T] {

  override def updateState(newState: S): PersistenceEffect[S] =
    PersistenceEffectImpl(UpdateState(newState))

  override def waitForInput(): TransitionalEffect[Done] =
    TransitionalEffectImpl(NoPersistence, Wait)

  override def transition[I](input: I, transitionTo: String): TransitionalEffect[Done] =
    TransitionalEffectImpl(NoPersistence, StepTransition(input, transitionTo))

  override def end(): TransitionalEffect[Done] =
    TransitionalEffectImpl(NoPersistence, End)

  override def reply[R](reply: R): Effect[R] =
    TransitionalEffectImpl(NoPersistence, Wait).thenReply(reply)

  override def reply[R](reply: R, metadata: Metadata): Effect[R] =
    TransitionalEffectImpl(NoPersistence, Wait).thenReply(reply, metadata)

  override def error[R](description: String): Effect.ErrorEffect[R] =
    ErrorEffectImpl(description, None)

  override def error[R](description: String, statusCode: Status.Code): Effect.ErrorEffect[R] =
    ErrorEffectImpl(description, Option(statusCode))

}
