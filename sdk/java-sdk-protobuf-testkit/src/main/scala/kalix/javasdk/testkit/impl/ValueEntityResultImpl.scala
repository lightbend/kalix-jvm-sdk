/*
 * Copyright 2024 Lightbend Inc.
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
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.javasdk.impl.valueentity.ValueEntityEffectImpl
import kalix.javasdk.testkit.DeferredCallDetails
import kalix.javasdk.testkit.ValueEntityResult
import kalix.javasdk.valueentity.ValueEntity
import java.util.{ List => JList }

import io.grpc.Status

import scala.jdk.CollectionConverters._

/**
 * INTERNAL API
 */
private[kalix] final class ValueEntityResultImpl[R](effect: ValueEntityEffectImpl[R]) extends ValueEntityResult[R] {

  def this(effect: ValueEntity.Effect[R]) =
    this(effect.asInstanceOf[ValueEntityEffectImpl[R]])

  private def secondaryEffect = effect.secondaryEffect

  override def isReply(): Boolean = effect.secondaryEffect.isInstanceOf[MessageReplyImpl[_]]

  private def secondaryEffectName: String = effect.secondaryEffect match {
    case _: MessageReplyImpl[_]   => "reply"
    case _: ForwardReplyImpl[_]   => "forward"
    case _: ErrorReplyImpl[_]     => "error"
    case _: NoSecondaryEffectImpl => "no effect" // this should never happen
  }

  override def getReply(): R = effect.secondaryEffect match {
    case reply: MessageReplyImpl[R @unchecked] => reply.message
    case _ => throw new IllegalStateException(s"The effect was not a reply but [$secondaryEffectName]")
  }

  override def isForward(): Boolean = effect.secondaryEffect.isInstanceOf[ForwardReplyImpl[_]]

  override def getForward(): DeferredCallDetails[_, R] = effect.secondaryEffect match {
    case reply: ForwardReplyImpl[R @unchecked] =>
      reply.deferredCall match {
        case t: GrpcDeferredCall[_, R @unchecked] => TestKitDeferredCall(t)
        case surprise =>
          throw new IllegalStateException(s"Unexpected type of service call in testkit: ${surprise.getClass.getName}")
      }
    case _ => throw new IllegalStateException(s"The effect was not a forward but [$secondaryEffectName]")
  }

  override def isError(): Boolean = effect.secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def getError(): String = effect.secondaryEffect match {
    case error: ErrorReplyImpl[_] => error.description
    case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
  }

  override def getErrorStatusCode: Status.Code = effect.secondaryEffect match {
    case ErrorReplyImpl(_, status, _) => status.getOrElse(Status.Code.UNKNOWN)
    case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
  }

  override def stateWasUpdated(): Boolean = effect.primaryEffect.isInstanceOf[ValueEntityEffectImpl.UpdateState[_]]

  override def getUpdatedState(): Any = effect.primaryEffect match {
    case ValueEntityEffectImpl.UpdateState(s) => s
    case _                                    => throw new IllegalStateException("State was not updated by the effect")
  }

  override def stateWasDeleted(): Boolean = effect.primaryEffect eq ValueEntityEffectImpl.DeleteEntity

  private def toDeferredCallDetails(sideEffects: Vector[SideEffect]): JList[DeferredCallDetails[_, _]] = {
    sideEffects
      .map { sideEffect =>
        TestKitDeferredCall(sideEffect.call.asInstanceOf[GrpcDeferredCall[_, _]])
          .asInstanceOf[DeferredCallDetails[_, _]] // java List is invariant in type
      }
      .toList
      .asJava
  }

  override def getSideEffects(): JList[DeferredCallDetails[_, _]] =
    toDeferredCallDetails(secondaryEffect.sideEffects)

}
