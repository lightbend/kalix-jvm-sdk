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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.ServiceCall
import com.akkaserverless.javasdk.SideEffect
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.ForwardReplyImpl
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.effect.NoReply
import com.akkaserverless.javasdk.impl.effect.NoSecondaryEffectImpl
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity.Effect

import java.util.{Collection => JCollection}
import scala.jdk.CollectionConverters._

object ReplicatedEntityEffectImpl {
  sealed trait PrimaryEffectImpl
  case object DeleteEntity extends PrimaryEffectImpl
  case object NoPrimaryEffect extends PrimaryEffectImpl
}

class ReplicatedEntityEffectImpl[R] extends Effect.Builder with Effect.OnSuccessBuilder with Effect[R] {
  import ReplicatedEntityEffectImpl._

  private var _primaryEffect: PrimaryEffectImpl = NoPrimaryEffect
  private var _secondaryEffect: SecondaryEffectImpl = NoSecondaryEffectImpl

  def primaryEffect: PrimaryEffectImpl = _primaryEffect

  def secondaryEffect: SecondaryEffectImpl = _secondaryEffect

  override def delete(): Effect.OnSuccessBuilder = {
    _primaryEffect = DeleteEntity
    this
  }

  override def reply[T](message: T): Effect[T] =
    reply(message, Metadata.EMPTY)

  override def reply[T](message: T, metadata: Metadata): Effect[T] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[Effect[T]]
  }

  override def forward[T](serviceCall: ServiceCall): Effect[T] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[Effect[T]]
  }

  override def error[T](description: String): Effect[T] = {
    _secondaryEffect = ErrorReplyImpl(description, _secondaryEffect.sideEffects)
    this.asInstanceOf[Effect[T]]
  }

  def hasError: Boolean =
    _secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def noReply[T](): Effect[T] = {
    _secondaryEffect = NoReply(_secondaryEffect.sideEffects)
    this.asInstanceOf[Effect[T]]
  }

  override def thenReply[T](message: T): Effect[T] =
    thenReply(message, Metadata.EMPTY)

  override def thenReply[T](message: T, metadata: Metadata): Effect[T] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[Effect[T]]
  }

  override def thenForward[T](serviceCall: ServiceCall): Effect[T] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[Effect[T]]
  }

  override def thenNoReply[T](): Effect[T] = {
    _secondaryEffect = NoReply(_secondaryEffect.sideEffects)
    this.asInstanceOf[Effect[T]]
  }

  override def addSideEffects(sideEffects: JCollection[SideEffect]): Effect[R] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects.asScala)
    this
  }

  override def addSideEffects(sideEffects: SideEffect*): Effect[R] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects)
    this
  }
}
