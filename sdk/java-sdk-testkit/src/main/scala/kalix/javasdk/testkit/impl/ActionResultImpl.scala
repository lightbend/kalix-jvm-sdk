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

package kalix.javasdk.testkit.impl

import kalix.javasdk.SideEffect
import kalix.javasdk.action.Action
import kalix.javasdk.impl.DeferredCallImpl
import kalix.javasdk.impl.action.ActionEffectImpl
import kalix.javasdk.testkit.ActionResult
import kalix.javasdk.testkit.DeferredCallDetails
import java.util.concurrent.CompletionStage
import java.util.{ List => JList }

import io.grpc.Status

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext

/**
 * INTERNAL API
 */
private[kalix] object ActionResultImpl {

  private def toDeferredCallDetails(sideEffects: Seq[SideEffect]): JList[DeferredCallDetails[_, _]] =
    sideEffects
      .map(s => TestKitDeferredCall(s.call.asInstanceOf[DeferredCallImpl[_, _]]): DeferredCallDetails[_, _])
      .asJava
}

/**
 * INTERNAL API
 */
final class ActionResultImpl[T](effect: ActionEffectImpl.PrimaryEffect[T]) extends ActionResult[T] {
  import ActionResultImpl._

  def this(effect: Action.Effect[T]) = this(effect.asInstanceOf[ActionEffectImpl.PrimaryEffect[T]])

  implicit val ec = ExecutionContext.Implicits.global

  /** @return true if the call had an effect with a reply, false if not */
  def isReply(): Boolean = effect.isInstanceOf[ActionEffectImpl.ReplyEffect[T]]

  def getReply(): T = {
    val reply = getEffectOfType(classOf[ActionEffectImpl.ReplyEffect[T]])
    reply.msg
  }

  //TODO add metadata??

  /** @return true if the call was forwarded, false if not */
  def isForward(): Boolean = effect.isInstanceOf[ActionEffectImpl.ForwardEffect[T]]

  def getForward(): DeferredCallDetails[Any, T] =
    effect match {
      case ActionEffectImpl.ForwardEffect(serviceCall: DeferredCallImpl[Any @unchecked, T @unchecked], _) =>
        TestKitDeferredCall(serviceCall)
      case _ =>
        throw new IllegalStateException(
          "expected effect type [ActionEffectImpl.ForwardEffect] but found [" + effect.getClass.getName + "]")
    }

  // TODO rewrite
  /** @return true if the call was async, false if not */
  def isAsync(): Boolean = effect.isInstanceOf[ActionEffectImpl.AsyncEffect[T]]

  def getAsyncResult(): CompletionStage[ActionResult[T]] = {
    val async = getEffectOfType(classOf[ActionEffectImpl.AsyncEffect[T]])
    async.effect.map(new ActionResultImpl(_).asInstanceOf[ActionResult[T]]).toJava
  }

  /** @return true if the call was an error, false if not */
  def isError(): Boolean = effect.isInstanceOf[ActionEffectImpl.ErrorEffect[T]]

  def getError(): String = {
    val error = getEffectOfType(classOf[ActionEffectImpl.ErrorEffect[T]])
    error.description
  }

  def getErrorStatusCode(): Status.Code = {
    val error = getEffectOfType(classOf[ActionEffectImpl.ErrorEffect[T]])
    error.statusCode.getOrElse(Status.Code.UNKNOWN)
  }

  /** @return true if the call had a noReply effect, false if not */
  def isNoReply(): Boolean = effect.isInstanceOf[ActionEffectImpl.NoReply[T]]

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

  def getSideEffects(): JList[DeferredCallDetails[_, _]] =
    toDeferredCallDetails(effect.internalSideEffects())

}
