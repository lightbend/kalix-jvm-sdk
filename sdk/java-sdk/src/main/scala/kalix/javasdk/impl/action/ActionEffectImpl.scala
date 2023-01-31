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

package kalix.javasdk.impl.action

import kalix.javasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.javasdk.action.Action

import java.util
import java.util.concurrent.CompletionStage
import io.grpc.Status
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.CompletionStageOps
import scala.jdk.FutureConverters.FutureOps
import scala.util.Failure
import scala.util.Success

/** INTERNAL API */
object ActionEffectImpl {
  sealed abstract class PrimaryEffect[T] extends Action.Effect[T] {
    override def addSideEffect(sideEffects: SideEffect*): Action.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects)
    override def addSideEffects(sideEffects: util.Collection[SideEffect]): Action.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects.asScala)
    override def canHaveSideEffects: Boolean = true
    def internalSideEffects(): Seq[SideEffect]
    protected def withSideEffects(sideEffects: Seq[SideEffect]): Action.Effect[T]
  }

  final case class ReplyEffect[T](msg: T, metadata: Option[Metadata], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ReplyEffect[T] =
      copy(internalSideEffects = sideEffects)
  }

  final case class AsyncEffect[T](effect: Future[Action.Effect[T]], internalSideEffects: Seq[SideEffect])
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

  object Builder extends Action.Effect.Builder {

    private class FirstOnlySubscriber[S] extends Subscriber[S] {
      private val promise = Promise[S]
      def toFuture: Future[S] = promise.future

      override def onSubscribe(s: Subscription): Unit = s.request(1) // we will be using only the first element to reply
      override def onNext(t: S): Unit = if (!promise.isCompleted) promise.complete(Success(t))
      override def onError(t: Throwable): Unit = if (!promise.isCompleted) promise.complete(Failure(t))
      override def onComplete(): Unit =
        if (!promise.isCompleted)
          promise.complete(Failure(throw new RuntimeException("Stream closed without any element received")))
    }

    def reply[S](message: S): Action.Effect[S] = ReplyEffect(message, None, Nil)
    def reply[S](message: S, metadata: Metadata): Action.Effect[S] = ReplyEffect(message, Some(metadata), Nil)
    def forward[S](serviceCall: DeferredCall[_, S]): Action.Effect[S] = ForwardEffect(serviceCall, Nil)
    def error[S](description: String): Action.Effect[S] = ErrorEffect(description, None, Nil)
    def error[S](description: String, statusCode: Status.Code): Action.Effect[S] = {
      if (statusCode.toStatus.isOk) throw new IllegalArgumentException("Cannot fail with a success status")
      ErrorEffect(description, Some(statusCode), Nil)
    }
    def asyncReply[S](futureMessage: CompletionStage[S]): Action.Effect[S] =
      AsyncEffect(futureMessage.asScala.map(s => Builder.reply[S](s))(ExecutionContext.parasitic), Nil)
    def asyncReply[S](messagePublisher: Publisher[S]): Action.Effect[S] = {
      val sub = new FirstOnlySubscriber[S]
      messagePublisher.subscribe(sub)
      asyncReply(sub.toFuture.asJava)
    }
    def asyncEffect[S](futureEffect: CompletionStage[Action.Effect[S]]): Action.Effect[S] =
      AsyncEffect(futureEffect.asScala, Nil)
    def asyncEffect[S](effectPublisher: Publisher[Action.Effect[S]]): Action.Effect[S] = {
      val sub = new FirstOnlySubscriber[Action.Effect[S]]
      effectPublisher.subscribe(sub)
      asyncEffect(sub.toFuture.asJava)
    }
    def ignore[S](): Action.Effect[S] =
      IgnoreEffect()
  }

  def builder(): Action.Effect.Builder = Builder

}
