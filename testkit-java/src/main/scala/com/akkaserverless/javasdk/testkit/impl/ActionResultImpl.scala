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

package com.akkaserverless.javasdk.testkit.impl

import com.akkaserverless.javasdk.testkit.ActionResult
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl
import com.akkaserverless.javasdk.action.Action
import com.akkaserverless.javasdk.ServiceCall
import java.util.concurrent.CompletionStage;
import scala.concurrent.Future;
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext

final class ActionResultImpl[T](effect: Action.Effect[T]) extends ActionResult[T] {

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

  def getForwardServiceCall(): ServiceCall = {
    val forward = getEffectOfType(classOf[ActionEffectImpl.ForwardEffect[T]])
    forward.serviceCall
  }

  // TODO rewrite
  /** @return true if the call was async, false if not */
  def isAsync(): Boolean = effect.isInstanceOf[ActionEffectImpl.AsyncEffect[T]]

  def getAsyncEffect(): CompletionStage[ActionResult[T]] = {
    val async = getEffectOfType(classOf[ActionEffectImpl.AsyncEffect[T]])
    async.effect.map(new ActionResultImpl(_).asInstanceOf[ActionResult[T]]).toJava
  }

  /** @return true if the call was an error, false if not */
  def isError(): Boolean = effect.isInstanceOf[ActionEffectImpl.ErrorEffect[T]]

  def getErrorDescription(): String = {
    val error = getEffectOfType(classOf[ActionEffectImpl.ErrorEffect[T]])
    error.description
  }

  /** @return true if the call had a noReply effect, false if not */
  def isNoReply(): Boolean = effect.isInstanceOf[ActionEffectImpl.NoReply[T]]

  /**
   * Look at the next effect and verify that it is of type E or fail if not or if there is no next effect. If successful
   * this consumes the effect, so that the next call to this method looks at the next effect from here.
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
}
