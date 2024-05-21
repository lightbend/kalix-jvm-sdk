/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import kalix.javasdk.impl.GrpcDeferredCall
import kalix.scalasdk.action.Action
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.action.ActionEffectImpl
import kalix.scalasdk.testkit.ActionResult
import kalix.scalasdk.testkit.DeferredCallDetails
import kalix.javasdk.impl.action.ActionEffectImpl.{ PrimaryEffect => JavaPrimaryEffect }
import io.grpc.Status

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class ActionResultImpl[T](val effect: ActionEffectImpl.PrimaryEffect[T]) extends ActionResult[T] {

  def this(effect: Action.Effect[T]) =
    this(effect.asInstanceOf[ActionEffectImpl.PrimaryEffect[T]])

  override def isReply: Boolean = effect.isInstanceOf[ActionEffectImpl.ReplyEffect[_]]
  override def reply: T = effect match {
    case e: ActionEffectImpl.ReplyEffect[T] => e.msg
    case _ => throw new IllegalStateException(s"The effect was not a reply but [$effectName]")
  }

  override def sideEffects: Seq[DeferredCallDetails[_, _]] =
    EffectUtils.toDeferredCallDetails(effect.toJavaSdk.asInstanceOf[JavaPrimaryEffect[_]].internalSideEffects)

  override def isForward: Boolean = effect.isInstanceOf[ActionEffectImpl.ForwardEffect[_]]

  override def forwardedTo: DeferredCallDetails[_, T] = effect match {
    case ActionEffectImpl.ForwardEffect(ScalaDeferredCallAdapter(deferredCall: GrpcDeferredCall[_, T]), _) =>
      TestKitDeferredCall(deferredCall)
    case _ => throw new IllegalStateException(s"The effect was not a forward but [$effectName]")
  }

  override def isAsync: Boolean = effect.isInstanceOf[ActionEffectImpl.AsyncEffect[_]]

  override def asyncResult: Future[ActionResult[T]] = effect match {
    case a: ActionEffectImpl.AsyncEffect[T] =>
      a.effect.map { case p: ActionEffectImpl.PrimaryEffect[T @unchecked] =>
        new ActionResultImpl[T](p)
      }(ExecutionContext.global)
    case _ => throw new IllegalStateException(s"The effect was not an async effect but [$effectName]")
  }

  override def isError: Boolean = effect.isInstanceOf[ActionEffectImpl.ErrorEffect[_]]

  override def isIgnore: Boolean = effect == ActionEffectImpl.IgnoreEffect()

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
      case _: ActionEffectImpl.ReplyEffect[_]    => "reply"
      case _: ActionEffectImpl.ForwardEffect[_]  => "forward"
      case _: ActionEffectImpl.ErrorEffect[_]    => "error"
      case _: ActionEffectImpl.AsyncEffect[_]    => "async effect"
      case _: ActionEffectImpl.IgnoreEffect.type => "ignore"
    }
}
