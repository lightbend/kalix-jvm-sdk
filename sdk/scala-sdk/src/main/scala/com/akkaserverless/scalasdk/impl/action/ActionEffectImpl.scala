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

package com.akkaserverless.scalasdk.impl.action

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.ServiceCall
import com.akkaserverless.scalasdk.SideEffect
import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.impl.JavaServiceCallAdapter
import com.akkaserverless.scalasdk.impl.JavaSideEffectAdapter

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
      val sideEffects = internalSideEffects.map { se => JavaSideEffectAdapter(se) }
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
      val sideEffects = internalSideEffects.map { se => JavaSideEffectAdapter(se) }
      val javaEffect = effect.flatMap(convertEffect)(ExecutionContext.parasitic)
      javasdk.impl.action.ActionEffectImpl.AsyncEffect(javaEffect, sideEffects)
    }
  }

  final case class ForwardEffect[T](serviceCall: ServiceCall[_, T], internalSideEffects: Seq[SideEffect])
      extends PrimaryEffect[T] {

    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ForwardEffect[T] =
      copy(internalSideEffects = sideEffects)

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val sideEffects = internalSideEffects.map { se => JavaSideEffectAdapter(se) }
      val javaServiceCall = JavaServiceCallAdapter(serviceCall)
      javasdk.impl.action.ActionEffectImpl.ForwardEffect(javaServiceCall, sideEffects)
    }
  }

  final case class ErrorEffect[T](description: String, internalSideEffects: Seq[SideEffect]) extends PrimaryEffect[T] {

    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ErrorEffect[T] =
      copy(internalSideEffects = sideEffects)

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val sideEffects = internalSideEffects.map { se => JavaSideEffectAdapter(se) }
      javasdk.impl.action.ActionEffectImpl.ErrorEffect(description, sideEffects)
    }
  }

  final case class NoReply[T](internalSideEffects: Seq[SideEffect]) extends PrimaryEffect[T] {

    def isEmpty: Boolean = true
    protected def withSideEffects(sideEffects: Seq[SideEffect]): NoReply[T] =
      copy(internalSideEffects = sideEffects)

    override def toJavaSdk: javasdk.impl.action.ActionEffectImpl.PrimaryEffect[T] = {
      val sideEffects = internalSideEffects.map { se => JavaSideEffectAdapter(se) }
      javasdk.impl.action.ActionEffectImpl.NoReply(sideEffects)
    }
  }

  object Builder extends Action.Effect.Builder {
    override def reply[S](message: S): Action.Effect[S] = ReplyEffect(message, None, Nil)
    override def reply[S](message: S, metadata: Metadata): Action.Effect[S] = ReplyEffect(message, Some(metadata), Nil)
    override def forward[S](serviceCall: ServiceCall[_, S]): Action.Effect[S] = ForwardEffect(serviceCall, Nil)
    override def noReply[S]: Action.Effect[S] = NoReply(Nil)
    override def error[S](description: String): Action.Effect[S] = ErrorEffect(description, Nil)
    override def asyncReply[S](futureMessage: Future[S]): Action.Effect[S] =
      AsyncEffect(futureMessage.map(s => Builder.reply[S](s))(ExecutionContext.parasitic), Nil)
    override def asyncEffect[S](futureEffect: Future[Action.Effect[S]]): Action.Effect[S] =
      AsyncEffect(futureEffect, Nil)
  }

  def builder(): Action.Effect.Builder = Builder

}
