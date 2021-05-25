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
  def _effects: List[Effect]
  override def effects(): util.Collection[Effect] = _effects.asJava
}

final case class MessageReplyImpl[T](payload: T, metadata: Metadata, _effects: List[Effect])
    extends MessageReply[T]
    with ReplyImpl[T] {
  def this(payload: T, metadata: Metadata) = this(payload, metadata, Nil)
  override def addEffects(effects: util.Collection[Effect]): MessageReply[T] = addEffects(effects.asScala)
  override def addEffects(effects: Effect*): MessageReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[Effect]): MessageReply[T] = copy(_effects = _effects ++ effects)
}

final case class ForwardReplyImpl[T](serviceCall: ServiceCall, _effects: List[Effect])
    extends ForwardReply[T]
    with ReplyImpl[T] {
  def this(serviceCall: ServiceCall) = this(serviceCall, Nil)
  override def addEffects(effects: util.Collection[Effect]): ForwardReply[T] = addEffects(effects.asScala)
  override def addEffects(effects: Effect*): ForwardReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[Effect]): ForwardReply[T] = copy(_effects = _effects ++ effects)
}

final case class FailureReplyImpl[T](description: String, _effects: List[Effect])
    extends FailureReply[T]
    with ReplyImpl[T] {
  def this(description: String) = this(description, Nil)
  override def addEffects(effects: util.Collection[Effect]): FailureReply[T] = addEffects(effects.asScala)
  override def addEffects(effects: Effect*): FailureReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[Effect]): FailureReply[T] = copy(_effects = _effects ++ effects)
}

final case class NoReply[T](_effects: List[Effect]) extends ReplyImpl[T] {
  override def isEmpty: Boolean = true
  override def addEffects(effects: util.Collection[Effect]): NoReply[T] = addEffects(effects.asScala)
  override def addEffects(effects: Effect*): NoReply[T] = addEffects(effects)
  private def addEffects(effects: Iterable[Effect]): NoReply[T] = copy(_effects = _effects ++ effects)
}

object NoReply {
  private val instance = NoReply[Any](Nil)
  def apply[T]: Reply[T] = instance.asInstanceOf[NoReply[T]]
}

final case class EffectImpl(serviceCall: ServiceCall, synchronous: Boolean) extends Effect
