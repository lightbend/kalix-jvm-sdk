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

package com.akkaserverless.javasdk.impl.reply

import com.akkaserverless.javasdk._
import com.akkaserverless.javasdk.reply._
import java.util
import scala.jdk.CollectionConverters._

sealed trait ReplyImpl[T] extends Reply[T] {
  override def isEmpty: Boolean = false
  def _effects: List[SideEffect]
  override def sideEffects(): util.Collection[SideEffect] = _effects.asJava
}

final case class MessageReplyImpl[T](payload: T, metadata: Metadata, _effects: List[SideEffect])
    extends MessageReply[T]
    with ReplyImpl[T] {
  def this(payload: T, metadata: Metadata) = this(payload, metadata, Nil)
  override def addSideEffects(effects: util.Collection[SideEffect]): MessageReply[T] = addEffects(effects.asScala)
  override def addSideEffects(effects: SideEffect*): MessageReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[SideEffect]): MessageReply[T] = copy(_effects = _effects ++ effects)
}

final case class ForwardReplyImpl[T](serviceCall: ServiceCall, _effects: List[SideEffect])
    extends ForwardReply[T]
    with ReplyImpl[T] {
  def this(serviceCall: ServiceCall) = this(serviceCall, Nil)
  override def addSideEffects(effects: util.Collection[SideEffect]): ForwardReply[T] = addEffects(effects.asScala)
  override def addSideEffects(effects: SideEffect*): ForwardReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[SideEffect]): ForwardReply[T] = copy(_effects = _effects ++ effects)
}

final case class ErrorReplyImpl[T](description: String, _effects: List[SideEffect])
    extends ErrorReply[T]
    with ReplyImpl[T] {
  def this(description: String) = this(description, Nil)
  override def addSideEffects(effects: util.Collection[SideEffect]): ErrorReply[T] = addEffects(effects.asScala)
  override def addSideEffects(effects: SideEffect*): ErrorReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[SideEffect]): ErrorReply[T] = copy(_effects = _effects ++ effects)
}

final case class NoReply[T](_effects: List[SideEffect]) extends ReplyImpl[T] {
  override def isEmpty: Boolean = true
  override def addSideEffects(effects: util.Collection[SideEffect]): NoReply[T] = addEffects(effects.asScala)
  override def addSideEffects(effects: SideEffect*): NoReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[SideEffect]): NoReply[T] = copy(_effects = _effects ++ effects)
}

object NoReply {
  private val instance = NoReply[Any](Nil)
  def apply[T]: Reply[T] = instance.asInstanceOf[NoReply[T]]
}

final case class SideEffectImpl(serviceCall: ServiceCall, synchronous: Boolean) extends SideEffect
