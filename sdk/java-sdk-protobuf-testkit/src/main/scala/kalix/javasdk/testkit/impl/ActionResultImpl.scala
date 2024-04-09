/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl

import kalix.javasdk.SideEffect
import kalix.javasdk.action.Action
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.action.ActionEffectImpl
import kalix.javasdk.testkit.ActionResult
import kalix.javasdk.testkit.DeferredCallDetails
import java.util.concurrent.CompletionStage
import java.util.{ List => JList }

import io.grpc.Status

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

/**
 * INTERNAL API
 */
private[kalix] object ActionResultImpl {

  private def toDeferredCallDetails(sideEffects: Seq[SideEffect]): JList[DeferredCallDetails[_, _]] =
    sideEffects
      .map(s => TestKitDeferredCall(s.call.asInstanceOf[GrpcDeferredCall[_, _]]): DeferredCallDetails[_, _])
      .asJava
}

/**
 * INTERNAL API
 */
final class ActionResultImpl[T](effect: ActionEffectImpl.PrimaryEffect[T]) extends ActionResult[T] {
  import ActionResultImpl._

  def this(effect: Action.Effect[T]) = this(effect.asInstanceOf[ActionEffectImpl.PrimaryEffect[T]])

  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  /** @return true if the call had an effect with a reply, false if not */
  override def isReply(): Boolean = effect.isInstanceOf[ActionEffectImpl.ReplyEffect[T]]

  override def getReply(): T = {
    val reply = getEffectOfType(classOf[ActionEffectImpl.ReplyEffect[T]])
    reply.msg
  }

  //TODO add metadata??

  /** @return true if the call was forwarded, false if not */
  override def isForward(): Boolean = effect.isInstanceOf[ActionEffectImpl.ForwardEffect[T]]

  override def getForward(): DeferredCallDetails[Any, T] =
    effect match {
      case ActionEffectImpl.ForwardEffect(serviceCall: GrpcDeferredCall[Any @unchecked, T @unchecked], _) =>
        TestKitDeferredCall(serviceCall)
      case _ =>
        throw new IllegalStateException(
          "expected effect type [ActionEffectImpl.ForwardEffect] but found [" + effect.getClass.getName + "]")
    }

  // TODO rewrite
  /** @return true if the call was async, false if not */
  override def isAsync(): Boolean = effect.isInstanceOf[ActionEffectImpl.AsyncEffect[T]]

  override def getAsyncResult(): CompletionStage[ActionResult[T]] = {
    val async = getEffectOfType(classOf[ActionEffectImpl.AsyncEffect[T]])
    async.effect.map(new ActionResultImpl(_).asInstanceOf[ActionResult[T]]).toJava
  }

  /** @return true if the call was an error, false if not */
  override def isError(): Boolean = effect.isInstanceOf[ActionEffectImpl.ErrorEffect[T]]

  override def getError(): String = {
    val error = getEffectOfType(classOf[ActionEffectImpl.ErrorEffect[T]])
    error.description
  }

  override def isIgnore(): Boolean = effect == ActionEffectImpl.IgnoreEffect()

  override def getErrorStatusCode(): Status.Code = {
    val error = getEffectOfType(classOf[ActionEffectImpl.ErrorEffect[T]])
    error.statusCode.getOrElse(Status.Code.UNKNOWN)
  }

  /**
   * Look at effect and verifies that it is of type E or fail if not.
   *
   * @return
   *   The next effect if it is of type E, for additional assertions.
   */
  private def getEffectOfType[E](expectedClass: Class[E]): E = {
    if (expectedClass.isInstance(effect)) effect.asInstanceOf[E]
    else
      throw new NoSuchElementException(
        "expected effect type [" + expectedClass.getName + "] but found [" + effect.getClass.getName + "]")
  }

  override def getSideEffects(): JList[DeferredCallDetails[_, _]] =
    toDeferredCallDetails(effect.internalSideEffects())

}
