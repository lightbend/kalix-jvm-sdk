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

package kalix.scalasdk.testkit.impl

import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.scalasdk.impl.valueentity.ValueEntityEffectImpl
import kalix.javasdk.impl.valueentity.{ ValueEntityEffectImpl => JValueEntityEffectImpl }
import kalix.scalasdk.testkit.DeferredCallDetails
import kalix.scalasdk.testkit.ValueEntityResult
import kalix.scalasdk.valueentity.ValueEntity
import io.grpc.Status

/**
 * INTERNAL API Used by the generated testkit
 */
final class ValueEntityResultImpl[R](effect: ValueEntityEffectImpl[R]) extends ValueEntityResult[R] {

  def this(effect: ValueEntity.Effect[R]) =
    this(effect.asInstanceOf[ValueEntityEffectImpl[R]])

  private def primaryEffect = effect.javasdkEffect.primaryEffect
  private def secondaryEffect = effect.javasdkEffect.secondaryEffect

  override def isReply: Boolean =
    secondaryEffect.isInstanceOf[MessageReplyImpl[_]]

  private def secondaryEffectName: String =
    EffectUtils.nameFor(secondaryEffect)

  override def reply: R =
    secondaryEffect match {
      case reply: MessageReplyImpl[R @unchecked] => reply.message
      case _ => throw new IllegalStateException(s"The effect was not a reply but [$secondaryEffectName]")
    }

  override def isForward: Boolean =
    secondaryEffect.isInstanceOf[ForwardReplyImpl[_]]

  override def forwardedTo: DeferredCallDetails[_, R] =
    EffectUtils.forwardDetailsFor(secondaryEffect)

  override def isError: Boolean =
    secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def errorDescription: String =
    secondaryEffect match {
      case error: ErrorReplyImpl[_] => error.description
      case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
    }

  override def errorStatusCode: Status.Code =
    secondaryEffect match {
      case error: ErrorReplyImpl[_] => error.status.getOrElse(Status.Code.UNKNOWN)
      case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
    }

  override def stateWasUpdated: Boolean =
    primaryEffect.isInstanceOf[JValueEntityEffectImpl.UpdateState[_]]

  override def updatedState: Any =
    primaryEffect match {
      case JValueEntityEffectImpl.UpdateState(s) => s
      case _ => throw new IllegalStateException("State was not updated by the effect")
    }

  override def stateWasDeleted: Boolean =
    primaryEffect eq JValueEntityEffectImpl.DeleteEntity

  override def sideEffects: Seq[DeferredCallDetails[_, _]] =
    EffectUtils.toDeferredCallDetails(secondaryEffect.sideEffects)

}
