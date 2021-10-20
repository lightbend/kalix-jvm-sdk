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

package com.akkaserverless.scalasdk.testkit.impl

import com.akkaserverless.scalasdk.SideEffect
import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.impl.action.ActionEffectImpl
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.akkaserverless.scalasdk.testkit.DeferredCallDetails
import com.akkaserverless.scalasdk.impl.JavaDeferredCallAdapter
import com.akkaserverless.scalasdk.ScalaDeferredCallAdapter

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ActionResultImpl[T](val effect: Action.Effect[T]) extends ActionResult[T] {
  override def isReply: Boolean = effect.isInstanceOf[ActionEffectImpl.ReplyEffect[_]]
  override def reply: T = effect match {
    case e: ActionEffectImpl.ReplyEffect[T] => e.msg
    case _ => throw new IllegalStateException(s"The effect was not a reply but [$effectName]")
  }

  private def extractServices(sideEffects: Seq[SideEffect]): Seq[DeferredCallDetails[T]] = {
    sideEffects.map { sideEffect =>
      sideEffect.serviceCall match {
        case ScalaDeferredCallAdapter(JavaDeferredCallAdapter(scalaSdkServiceCall)) =>
          scalaSdkServiceCall.asInstanceOf[DeferredCallDetails[T]]
      }
    }
  }

  override def sideEffects: Seq[DeferredCallDetails[T]] = effect match {
    case ActionEffectImpl.ReplyEffect(_, _, internalSideEffects) => extractServices(internalSideEffects)
    case ActionEffectImpl.ForwardEffect(_, internalSideEffects)  => extractServices(internalSideEffects)
    case ActionEffectImpl.AsyncEffect(_, internalSideEffects)    => extractServices(internalSideEffects)
    case ActionEffectImpl.ErrorEffect(_, internalSideEffects)    => extractServices(internalSideEffects)
    case ActionEffectImpl.NoReply(internalSideEffects)           => extractServices(internalSideEffects)
  }

  override def isForward: Boolean = effect.isInstanceOf[ActionEffectImpl.ForwardEffect[_]]

  override def forwardedTo: DeferredCallDetails[T] = effect match {
    case e: ActionEffectImpl.ForwardEffect[T] => e.serviceCall.asInstanceOf[DeferredCallDetails[T]]
    case _ => throw new IllegalStateException(s"The effect was not a forward but [$effectName]")
  }

  override def isAsync: Boolean = effect.isInstanceOf[ActionEffectImpl.AsyncEffect[_]]

  override def asyncResult: Future[ActionResult[T]] = effect match {
    case a: ActionEffectImpl.AsyncEffect[T] =>
      a.effect.map { case p: ActionEffectImpl.PrimaryEffect[T] =>
        new ActionResultImpl[T](p)
      }(ExecutionContext.global)
    case _ => throw new IllegalStateException(s"The effect was not an async effect but [$effectName]")
  }

  override def isError: Boolean = effect.isInstanceOf[ActionEffectImpl.ErrorEffect[_]]

  override def errorDescription: String = effect match {
    case e: ActionEffectImpl.ErrorEffect[_] => e.description
    case _ => throw new IllegalStateException(s"The effect was not error but [$effectName]")
  }

  override def isNoReply: Boolean = effect.isInstanceOf[ActionEffectImpl.NoReply[_]]

  private def effectName: String =
    effect match {
      case _: ActionEffectImpl.ReplyEffect[_]   => "reply"
      case _: ActionEffectImpl.ForwardEffect[_] => "forward"
      case _: ActionEffectImpl.ErrorEffect[_]   => "error"
      case _: ActionEffectImpl.NoReply[_]       => "noReply"
      case _: ActionEffectImpl.AsyncEffect[_]   => "async effect"
    }
}
