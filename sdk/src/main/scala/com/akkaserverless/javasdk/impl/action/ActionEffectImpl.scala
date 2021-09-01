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

package com.akkaserverless.javasdk.impl.action

import akka.dispatch.ExecutionContexts
import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.ServiceCall
import com.akkaserverless.javasdk.SideEffect
import com.akkaserverless.javasdk.action.Action

import java.util
import java.util.concurrent.CompletionStage
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.CompletionStageOps

object ActionEffectImpl {
  sealed abstract class PrimaryEffect[T] extends Action.Effect[T] {
    override def addSideEffect(sideEffects: SideEffect*): Action.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects)
    override def addSideEffects(sideEffects: util.Collection[SideEffect]): Action.Effect[T] =
      withSideEffects(internalSideEffects() ++ sideEffects.asScala)

    protected def internalSideEffects(): immutable.Seq[SideEffect]
    protected def withSideEffects(sideEffects: immutable.Seq[SideEffect]): Action.Effect[T]
  }
  final case class ReplyEffect[T](msg: T, metadata: Option[Metadata], internalSideEffects: immutable.Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ReplyEffect[T] =
      copy(internalSideEffects = sideEffects)
  }
  final case class AsyncEffect[T](effect: Future[Action.Effect[T]], internalSideEffects: immutable.Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): AsyncEffect[T] =
      copy(internalSideEffects = sideEffects)
  }
  final case class ForwardEffect[T](serviceCall: ServiceCall, internalSideEffects: immutable.Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ForwardEffect[T] =
      copy(internalSideEffects = sideEffects)
  }
  final case class ErrorEffect[T](description: String, internalSideEffects: immutable.Seq[SideEffect])
      extends PrimaryEffect[T] {
    def isEmpty: Boolean = false
    protected def withSideEffects(sideEffects: Seq[SideEffect]): ErrorEffect[T] =
      copy(internalSideEffects = sideEffects)
  }
  final case class NoReply[T](internalSideEffects: immutable.Seq[SideEffect]) extends PrimaryEffect[T] {
    def isEmpty: Boolean = true
    protected def withSideEffects(sideEffects: Seq[SideEffect]): NoReply[T] = copy(internalSideEffects = sideEffects)
  }

  object Builder extends Action.Effect.Builder {
    def reply[S](message: S): Action.Effect[S] = ReplyEffect(message, None, Nil)
    def reply[S](message: S, metadata: Metadata): Action.Effect[S] = ReplyEffect(message, Some(metadata), Nil)
    def forward[S](serviceCall: ServiceCall): Action.Effect[S] = ForwardEffect(serviceCall, Nil)
    def noReply[S](): Action.Effect[S] = NoReply(Nil)
    def error[S](description: String): Action.Effect[S] = ErrorEffect(description, Nil)
    def asyncReply[S](futureMessage: CompletionStage[S]): Action.Effect[S] = AsyncEffect(
      futureMessage.asScala.map(s => Builder.reply[S](s))(ExecutionContext.parasitic),
      Nil
    )
    def asyncEffect[S](futureEffect: CompletionStage[Action.Effect[S]]): Action.Effect[S] =
      AsyncEffect(
        futureEffect.asScala,
        Nil
      )
  }

  def builder(): Action.Effect.Builder = Builder

}
