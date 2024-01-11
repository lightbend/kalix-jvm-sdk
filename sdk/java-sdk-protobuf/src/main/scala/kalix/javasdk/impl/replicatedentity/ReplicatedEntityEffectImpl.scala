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

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.javasdk.impl.effect.SecondaryEffectImpl
import kalix.javasdk.replicatedentity.ReplicatedEntity.Effect

import java.util.{ Collection => JCollection }
import scala.jdk.CollectionConverters._
import kalix.replicatedentity.ReplicatedData
import io.grpc.Status
import kalix.javasdk.StatusCode.ErrorCode
import kalix.javasdk.impl.StatusCodeConverter

object ReplicatedEntityEffectImpl {
  sealed trait PrimaryEffectImpl
  case class UpdateData(newData: ReplicatedData) extends PrimaryEffectImpl
  case object DeleteEntity extends PrimaryEffectImpl
  case object NoPrimaryEffect extends PrimaryEffectImpl
}

class ReplicatedEntityEffectImpl[D <: ReplicatedData, R]
    extends Effect.Builder[D]
    with Effect.OnSuccessBuilder
    with Effect[R] {
  import ReplicatedEntityEffectImpl._

  private var _primaryEffect: PrimaryEffectImpl = NoPrimaryEffect
  private var _secondaryEffect: SecondaryEffectImpl = NoSecondaryEffectImpl()

  def primaryEffect: PrimaryEffectImpl = _primaryEffect

  def secondaryEffect: SecondaryEffectImpl = _secondaryEffect

  override def update(newData: D): ReplicatedEntityEffectImpl[D, R] = {
    _primaryEffect = UpdateData(newData)
    this
  }

  override def delete(): ReplicatedEntityEffectImpl[D, R] = {
    _primaryEffect = DeleteEntity
    this
  }

  override def reply[T](message: T): ReplicatedEntityEffectImpl[D, T] =
    reply(message, Metadata.EMPTY)

  override def reply[T](message: T, metadata: Metadata): ReplicatedEntityEffectImpl[D, T] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[ReplicatedEntityEffectImpl[D, T]]
  }

  override def forward[T](serviceCall: DeferredCall[_, T]): ReplicatedEntityEffectImpl[D, T] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[ReplicatedEntityEffectImpl[D, T]]
  }

  override def error[T](description: String): ReplicatedEntityEffectImpl[D, T] = {
    _secondaryEffect = ErrorReplyImpl(description, None, _secondaryEffect.sideEffects)
    this.asInstanceOf[ReplicatedEntityEffectImpl[D, T]]
  }

  override def error[T](description: String, grpcErrorCode: Status.Code): ReplicatedEntityEffectImpl[D, T] = {
    if (grpcErrorCode.toStatus.isOk) throw new IllegalArgumentException("Cannot fail with a success status")
    _secondaryEffect = ErrorReplyImpl(description, Some(grpcErrorCode), _secondaryEffect.sideEffects)
    this.asInstanceOf[ReplicatedEntityEffectImpl[D, T]]
  }

  override def error[T](description: String, httpErrorCode: ErrorCode): ReplicatedEntityEffectImpl[D, T] = {
    _secondaryEffect =
      ErrorReplyImpl(description, Some(StatusCodeConverter.toGrpcCode(httpErrorCode)), _secondaryEffect.sideEffects)
    this.asInstanceOf[ReplicatedEntityEffectImpl[D, T]]
  }

  def hasError: Boolean =
    _secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def thenReply[T](message: T): ReplicatedEntityEffectImpl[D, T] =
    thenReply(message, Metadata.EMPTY)

  override def thenReply[T](message: T, metadata: Metadata): ReplicatedEntityEffectImpl[D, T] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[ReplicatedEntityEffectImpl[D, T]]
  }

  override def thenForward[T](serviceCall: DeferredCall[_, T]): ReplicatedEntityEffectImpl[D, T] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[ReplicatedEntityEffectImpl[D, T]]
  }

  override def addSideEffects(sideEffects: JCollection[SideEffect]): ReplicatedEntityEffectImpl[D, R] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects.asScala)
    this
  }

  override def addSideEffects(sideEffects: SideEffect*): ReplicatedEntityEffectImpl[D, R] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects)
    this
  }
}
