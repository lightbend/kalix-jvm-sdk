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

package kalix.javasdk.impl.action

import kalix.javasdk.{ DeferredCall, Metadata, SideEffect }
import java.util
import java.util.concurrent.CompletionStage

import io.grpc.Status
import kalix.javasdk.StatusCode.ErrorCode
import kalix.javasdk.impl.StatusCodeConverter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.CompletionStageOps

import kalix.javasdk.HttpResponse
import kalix.javasdk.action.AbstractAction

/** INTERNAL API */
object ActionEffectImpl {
  sealed abstract class PrimaryEffect[T] extends AbstractAction.Effect[T] {
    override def addSideEffect(sideEffects: SideEffect*): AbstractAction.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects)
    override def addSideEffects(sideEffects: util.Collection[SideEffect]): AbstractAction.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects.asScala)
    override def canHaveSideEffects: Boolean = true
    def internalSideEffects(): Seq[SideEffect]
    protected def withSideEffects(sideEffects: Seq[SideEffect]): AbstractAction.Effect[T]
  }

  final case class ReplyEffect[T](msg: T, metadata: Option[Metadata], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ReplyEffect[T] =
      copy(internalSideEffects = sideEffects)
  }

  final case class AsyncEffect[T](effect: Future[AbstractAction.Effect[T]], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): AsyncEffect[T] =
      copy(internalSideEffects = sideEffects)
  }

  final case class ForwardEffect[T](serviceCall: DeferredCall[_, T], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ForwardEffect[T] =
      copy(internalSideEffects = sideEffects)
  }

  final case class ErrorEffect[T](
      description: String,
      statusCode: Option[Status.Code],
      internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ErrorEffect[T] =
      copy(internalSideEffects = sideEffects)
  }

  def IgnoreEffect[T](): PrimaryEffect[T] = IgnoreEffect.asInstanceOf[PrimaryEffect[T]]
  case object IgnoreEffect extends PrimaryEffect[Nothing] {
    def isEmpty: Boolean = true
    override def canHaveSideEffects: Boolean = false

    override def internalSideEffects() = Nil

    protected def withSideEffects(sideEffect: Seq[SideEffect]): PrimaryEffect[Nothing] = {
      throw new IllegalArgumentException("adding side effects to 'ignore' is not allowed.")
    }
  }

  object Builder extends AbstractAction.Effect.Builder {
    def reply[S](message: S): AbstractAction.Effect[S] = {
      message match {
        case httpResponse: HttpResponse =>
          ReplyEffect(message, Some(Metadata.EMPTY.withStatusCode(httpResponse.getStatusCode)), Nil)
        case _ => ReplyEffect(message, None, Nil)
      }
    }
    def reply[S](message: S, metadata: Metadata): AbstractAction.Effect[S] = {
      message match {
        case httpResponse: HttpResponse =>
          ReplyEffect(message, Some(metadata.withStatusCode(httpResponse.getStatusCode)), Nil)
        case _ => ReplyEffect(message, Some(metadata), Nil)
      }
      ReplyEffect(message, Some(metadata), Nil)
    }
    def forward[S](serviceCall: DeferredCall[_, S]): AbstractAction.Effect[S] = ForwardEffect(serviceCall, Nil)
    def error[S](description: String): AbstractAction.Effect[S] = ErrorEffect(description, None, Nil)
    def error[S](description: String, grpcErrorCode: Status.Code): AbstractAction.Effect[S] = {
      if (grpcErrorCode.toStatus.isOk) throw new IllegalArgumentException("Cannot fail with a success status")
      ErrorEffect(description, Some(grpcErrorCode), Nil)
    }
    def error[S](description: String, httpErrorCode: ErrorCode): AbstractAction.Effect[S] =
      error(description, StatusCodeConverter.toGrpcCode(httpErrorCode))
    def asyncReply[S](futureMessage: CompletionStage[S]): AbstractAction.Effect[S] =
      asyncReply(futureMessage, Metadata.EMPTY)
    def asyncReply[S](futureMessage: CompletionStage[S], metadata: Metadata): AbstractAction.Effect[S] =
      AsyncEffect(futureMessage.asScala.map(s => Builder.reply[S](s, metadata))(ExecutionContext.parasitic), Nil)
    def asyncEffect[S](futureEffect: CompletionStage[AbstractAction.Effect[S]]): AbstractAction.Effect[S] =
      AsyncEffect(futureEffect.asScala, Nil)
    def ignore[S](): AbstractAction.Effect[S] =
      IgnoreEffect()
  }

  def builder(): AbstractAction.Effect.Builder = Builder

}
