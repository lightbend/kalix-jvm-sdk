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

package kalix.scalasdk.impl.action

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import kalix.javasdk
import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.action.Action
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.ScalaSideEffectAdapter
import io.grpc.Status

private[scalasdk] object ActionEffectImpl {

  sealed abstract class PrimaryEffect[T] extends Action.Effect[T] {

    def toJavaSdk: javasdk.action.Action.Effect[T]

    override def addSideEffect(sideEffects: SideEffect*): Action.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects)
    override def addSideEffects(sideEffects: Seq[SideEffect]): Action.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects)

    protected def internalSideEffects(): Seq[SideEffect]
    protected def withSideEffects(sideEffects: Seq[SideEffect]): Action.Effect[T]
  }

  final case class ReplyEffect[T](msg: T, metadata: Option[Metadata], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {

    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ReplyEffect[T] =
      copy(internalSideEffects = sideEffects)

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val metadataUnwrapped: Option[javasdk.Metadata] = metadata.map(_.impl)
      val sideEffects = internalSideEffects.map { case ScalaSideEffectAdapter(se) => se }
      javasdk.impl.action.ActionEffectImpl.ReplyEffect(msg, metadataUnwrapped, sideEffects)
    }
  }

  final case class AsyncEffect[T](effect: Future[Action.Effect[T]], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {

    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): AsyncEffect[T] =
      copy(internalSideEffects = sideEffects)

    private def convertEffect(effect: Action.Effect[T]): Future[javasdk.action.Action.Effect[T]] = {
      effect match {
        case eff: AsyncEffect[T] =>
          // FIXME? the Future may wrap another AsyncEffect.
          //  Should we put a limit on it to avoid a stackoverflow?
          eff.effect.flatMap(convertEffect)(ExecutionContext.parasitic)
        case eff: PrimaryEffect[T] => Future.successful(eff.toJavaSdk)
      }
    }

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val sideEffects = internalSideEffects.map { case ScalaSideEffectAdapter(javasdkSideEffect) => javasdkSideEffect }
      val javaEffect = effect.flatMap(convertEffect)(ExecutionContext.parasitic)
      javasdk.impl.action.ActionEffectImpl.AsyncEffect(javaEffect, sideEffects)
    }
  }

  final case class ForwardEffect[T](serviceCall: DeferredCall[_, T], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {

    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ForwardEffect[T] =
      copy(internalSideEffects = sideEffects)

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val sideEffects = internalSideEffects.map { case ScalaSideEffectAdapter(se) => se }
      val javaDeferredCall = serviceCall match {
        case ScalaDeferredCallAdapter(jdc) => jdc
      }
      javasdk.impl.action.ActionEffectImpl.ForwardEffect(javaDeferredCall, sideEffects)
    }
  }

  final case class ErrorEffect[T](
      description: String,
      statusCode: Option[Status.Code],
      internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {

    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ErrorEffect[T] =
      copy(internalSideEffects = sideEffects)

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val sideEffects = internalSideEffects.map { case ScalaSideEffectAdapter(jse) => jse }
      javasdk.impl.action.ActionEffectImpl
        .ErrorEffect(description, statusCode, sideEffects)
    }
  }

  final case class NoReply[T](internalSideEffects: Seq[SideEffect]) extends PrimaryEffect[T] {

    def isEmpty: Boolean = true
    protected def withSideEffects(sideEffects: Seq[SideEffect]): NoReply[T] =
      copy(internalSideEffects = sideEffects)

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val sideEffects = internalSideEffects.map { case ScalaSideEffectAdapter(jse) => jse }
      javasdk.impl.action.ActionEffectImpl.NoReply(sideEffects)
    }
  }

  object Builder extends Action.Effect.Builder {
    override def reply[S](message: S): Action.Effect[S] = ReplyEffect(message, None, Nil)
    override def reply[S](message: S, metadata: Metadata): Action.Effect[S] = ReplyEffect(message, Some(metadata), Nil)
    override def forward[S](serviceCall: DeferredCall[_, S]): Action.Effect[S] = ForwardEffect(serviceCall, Nil)
    override def noReply[S]: Action.Effect[S] = NoReply(Nil)
    override def error[S](description: String, statusCode: Option[Status.Code]): Action.Effect[S] = {
      if (statusCode.exists(_.toStatus.isOk)) throw new IllegalArgumentException("Cannot fail with a success status")
      ErrorEffect(description, statusCode, Nil)
    }
    override def asyncReply[S](futureMessage: Future[S]): Action.Effect[S] =
      AsyncEffect(futureMessage.map(s => Builder.reply[S](s))(ExecutionContext.parasitic), Nil)
    override def asyncEffect[S](futureEffect: Future[Action.Effect[S]]): Action.Effect[S] =
      AsyncEffect(futureEffect, Nil)
  }

  def builder(): Action.Effect.Builder = Builder

}
