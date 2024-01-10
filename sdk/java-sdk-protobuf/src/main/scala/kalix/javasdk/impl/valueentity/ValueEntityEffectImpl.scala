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

package kalix.javasdk.impl.valueentity

import java.util
import scala.jdk.CollectionConverters._
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.javasdk.impl.effect.SecondaryEffectImpl
import kalix.javasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.javasdk.valueentity.ValueEntity.Effect
import Effect.Builder
import Effect.OnSuccessBuilder
import io.grpc.Status
import kalix.javasdk.StatusCode.ErrorCode
import kalix.javasdk.impl.StatusCodeConverter

object ValueEntityEffectImpl {
  sealed trait PrimaryEffectImpl[+S]
  final case class UpdateState[S](newState: S) extends PrimaryEffectImpl[S]
  case object DeleteEntity extends PrimaryEffectImpl[Nothing]
  case object NoPrimaryEffect extends PrimaryEffectImpl[Nothing]
}

class ValueEntityEffectImpl[S] extends Builder[S] with OnSuccessBuilder[S] with Effect[S] {
  import ValueEntityEffectImpl._

  private var _primaryEffect: PrimaryEffectImpl[S] = NoPrimaryEffect
  private var _secondaryEffect: SecondaryEffectImpl = NoSecondaryEffectImpl()

  def primaryEffect: PrimaryEffectImpl[S] = _primaryEffect

  def secondaryEffect: SecondaryEffectImpl = _secondaryEffect

  override def updateState(newState: S): ValueEntityEffectImpl[S] = {
    _primaryEffect = UpdateState(newState)
    this
  }

  override def deleteEntity(): ValueEntityEffectImpl[S] = {
    _primaryEffect = DeleteEntity
    this
  }

  override def deleteState(): ValueEntityEffectImpl[S] =
    deleteEntity()

  override def reply[T](message: T): ValueEntityEffectImpl[T] =
    reply(message, Metadata.EMPTY)

  override def reply[T](message: T, metadata: Metadata): ValueEntityEffectImpl[T] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[ValueEntityEffectImpl[T]]
  }

  override def forward[T](serviceCall: DeferredCall[_, T]): ValueEntityEffectImpl[T] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[ValueEntityEffectImpl[T]]
  }

  override def error[T](description: String): ValueEntityEffectImpl[T] = {
    _secondaryEffect = ErrorReplyImpl(description, None, _secondaryEffect.sideEffects)
    this.asInstanceOf[ValueEntityEffectImpl[T]]
  }

  override def error[T](description: String, grpcErrorCode: Status.Code): ValueEntityEffectImpl[T] = {
    if (grpcErrorCode.toStatus.isOk) throw new IllegalArgumentException("Cannot fail with a success status")
    _secondaryEffect = ErrorReplyImpl(description, Some(grpcErrorCode), _secondaryEffect.sideEffects)
    this.asInstanceOf[ValueEntityEffectImpl[T]]
  }

  override def error[T](description: String, httpErrorCode: ErrorCode): ValueEntityEffectImpl[T] = {
    _secondaryEffect =
      ErrorReplyImpl(description, Some(StatusCodeConverter.toGrpcCode(httpErrorCode)), _secondaryEffect.sideEffects)
    this.asInstanceOf[ValueEntityEffectImpl[T]]
  }

  def hasError(): Boolean =
    _secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def thenReply[T](message: T): ValueEntityEffectImpl[T] =
    thenReply(message, Metadata.EMPTY)

  override def thenReply[T](message: T, metadata: Metadata): ValueEntityEffectImpl[T] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[ValueEntityEffectImpl[T]]
  }

  override def thenForward[T](serviceCall: DeferredCall[_, T]): ValueEntityEffectImpl[T] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[ValueEntityEffectImpl[T]]
  }

  override def addSideEffects(sideEffects: util.Collection[SideEffect]): ValueEntityEffectImpl[S] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects.asScala)
    this
  }

  override def addSideEffects(sideEffects: SideEffect*): ValueEntityEffectImpl[S] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects)
    this
  }
}
