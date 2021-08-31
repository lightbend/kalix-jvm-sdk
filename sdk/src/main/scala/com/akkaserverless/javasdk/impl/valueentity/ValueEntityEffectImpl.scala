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

package com.akkaserverless.javasdk.impl.valueentity

import java.util

import scala.jdk.CollectionConverters._

import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.ServiceCall
import com.akkaserverless.javasdk.SideEffect
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.ForwardReplyImpl
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.effect.NoReply
import com.akkaserverless.javasdk.impl.effect.NoSecondaryEffectImpl
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.valueentity.ValueEntity.Effect
import Effect.Builder
import Effect.OnSuccessBuilder

object ValueEntityEffectImpl {
  sealed trait PrimaryEffectImpl[+S]
  final case class UpdateState[S](newState: S) extends PrimaryEffectImpl[S]
  case object DeleteState extends PrimaryEffectImpl[Nothing]
  case object NoPrimaryEffect extends PrimaryEffectImpl[Nothing]
}

class ValueEntityEffectImpl[S] extends Builder[S] with OnSuccessBuilder[S] with Effect[S] {
  import ValueEntityEffectImpl._

  private var _primaryEffect: PrimaryEffectImpl[S] = NoPrimaryEffect
  private var _secondaryEffect: SecondaryEffectImpl = NoSecondaryEffectImpl

  def primaryEffect: PrimaryEffectImpl[S] = _primaryEffect

  def secondaryEffect: SecondaryEffectImpl = _secondaryEffect

  override def updateState(newState: S): OnSuccessBuilder[S] = {
    _primaryEffect = UpdateState(newState)
    this
  }

  override def deleteState(): OnSuccessBuilder[S] = {
    _primaryEffect = DeleteState
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

  def hasError(): Boolean =
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

  override def addSideEffects(sideEffects: util.Collection[SideEffect]): Effect[S] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects.asScala)
    this
  }

  override def addSideEffects(sideEffects: SideEffect*): Effect[S] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects)
    this
  }
}
