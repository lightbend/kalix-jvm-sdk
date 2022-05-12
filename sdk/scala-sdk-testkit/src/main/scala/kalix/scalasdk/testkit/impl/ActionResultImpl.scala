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

package kalix.scalasdk.testkit.impl

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import io.grpc.Status
import kalix.javasdk.impl.DeferredCallImpl
import kalix.javasdk.impl.action.ActionEffectImpl.{ PrimaryEffect => JavaPrimaryEffect }
import kalix.scalasdk.action.Action
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.action.ActionEffectImpl
import kalix.scalasdk.testkit.ActionResult
import kalix.scalasdk.testkit.DeferredCallDetails
import kalix.scalasdk.testkit.SingleTimerDetails

final class ActionResultImpl[T](val effect: ActionEffectImpl.PrimaryEffect[T], timerScheduler: TestKitTimerScheduler)
    extends ActionResult[T] {

  def this(effect: Action.Effect[T], timerScheduler: TestKitTimerScheduler) =
    this(effect.asInstanceOf[ActionEffectImpl.PrimaryEffect[T]], timerScheduler)

  override val timerCancellations: Seq[String] = timerScheduler.timerCancellations
  override val singleTimerDetails: Seq[SingleTimerDetails] = timerScheduler.singleTimers

  private lazy val singleTimersIterator = singleTimerDetails.iterator

  override def nextSingleTimerDetails: SingleTimerDetails =
    if (!singleTimersIterator.hasNext) throw new NoSuchElementException("No more timers found")
    else singleTimersIterator.next()

  override def isReply: Boolean = effect.isInstanceOf[ActionEffectImpl.ReplyEffect[_]]
  override def reply: T = effect match {
    case e: ActionEffectImpl.ReplyEffect[T] => e.msg
    case _ => throw new IllegalStateException(s"The effect was not a reply but [$effectName]")
  }

  override def sideEffects: Seq[DeferredCallDetails[_, _]] =
    EffectUtils.toDeferredCallDetails(effect.toJavaSdk.asInstanceOf[JavaPrimaryEffect[_]].internalSideEffects())

  override def isForward: Boolean = effect.isInstanceOf[ActionEffectImpl.ForwardEffect[_]]

  override def forwardedTo: DeferredCallDetails[_, T] = effect match {
    case ActionEffectImpl.ForwardEffect(ScalaDeferredCallAdapter(deferredCall: DeferredCallImpl[_, T]), _) =>
      TestKitDeferredCall(deferredCall)
    case _ => throw new IllegalStateException(s"The effect was not a forward but [$effectName]")
  }

  override def isAsync: Boolean = effect.isInstanceOf[ActionEffectImpl.AsyncEffect[_]]

  override def asyncResult: Future[ActionResult[T]] = effect match {
    case a: ActionEffectImpl.AsyncEffect[T] =>
      a.effect.map { case p: ActionEffectImpl.PrimaryEffect[T] =>
        new ActionResultImpl[T](p, timerScheduler)
      }(ExecutionContext.global)
    case _ => throw new IllegalStateException(s"The effect was not an async effect but [$effectName]")
  }

  override def isError: Boolean = effect.isInstanceOf[ActionEffectImpl.ErrorEffect[_]]

  override def errorDescription: String = effect match {
    case e: ActionEffectImpl.ErrorEffect[_] => e.description
    case _ => throw new IllegalStateException(s"The effect was not an error but [$effectName]")
  }

  override def errorStatusCode: Status.Code = effect match {
    case e: ActionEffectImpl.ErrorEffect[_] => e.statusCode.getOrElse(Status.Code.UNKNOWN)
    case _ => throw new IllegalStateException(s"The effect was not an error but [$effectName]")
  }

  private def effectName: String =
    effect match {
      case _: ActionEffectImpl.ReplyEffect[_]   => "reply"
      case _: ActionEffectImpl.ForwardEffect[_] => "forward"
      case _: ActionEffectImpl.ErrorEffect[_]   => "error"
      case _: ActionEffectImpl.AsyncEffect[_]   => "async effect"
    }

}
