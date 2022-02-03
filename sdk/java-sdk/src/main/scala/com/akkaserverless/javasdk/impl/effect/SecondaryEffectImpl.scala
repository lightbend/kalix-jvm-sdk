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

package com.akkaserverless.javasdk.impl.effect

import com.akkaserverless.javasdk.DeferredCall
import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.SideEffect
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.impl.DeferredCallImpl
import com.akkaserverless.javasdk.impl.effect
import com.akkaserverless.protocol.component.ClientAction
import com.google.protobuf.{ Any => JavaPbAny }

sealed trait SecondaryEffectImpl {
  def sideEffects: Vector[SideEffect]
  def addSideEffects(sideEffects: Iterable[SideEffect]): SecondaryEffectImpl

  final def replyToClientAction(
      anySupport: AnySupport,
      commandId: Long,
      allowNoReply: Boolean): Option[ClientAction] = {
    this match {
      case message: effect.MessageReplyImpl[JavaPbAny] @unchecked =>
        Some(ClientAction(ClientAction.Action.Reply(EffectSupport.asProtocol(message))))
      case forward: effect.ForwardReplyImpl[JavaPbAny] @unchecked =>
        Some(ClientAction(ClientAction.Action.Forward(EffectSupport.asProtocol(anySupport, forward))))
      case failure: effect.ErrorReplyImpl[JavaPbAny] @unchecked =>
        Some(
          ClientAction(
            ClientAction.Action
              .Failure(com.akkaserverless.protocol.component.Failure(commandId, failure.description))))
      case _: effect.NoReply[_] @unchecked | effect.NoSecondaryEffectImpl =>
        if (allowNoReply) {
          None
        } else {
          throw new RuntimeException("No reply or forward returned by command handler!")
        }
    }
  }
}

case object NoSecondaryEffectImpl extends SecondaryEffectImpl {
  override def sideEffects: Vector[SideEffect] = Vector.empty

  override def addSideEffects(sideEffects: Iterable[SideEffect]): SecondaryEffectImpl =
    NoReply(sideEffects.toVector)
}

final case class MessageReplyImpl[T](message: T, metadata: Metadata, sideEffects: Vector[SideEffect])
    extends SecondaryEffectImpl {

  override def addSideEffects(newSideEffects: Iterable[SideEffect]): SecondaryEffectImpl =
    copy(sideEffects = sideEffects ++ newSideEffects)
}

final case class ForwardReplyImpl[T](deferredCall: DeferredCall[_, T], sideEffects: Vector[SideEffect])
    extends SecondaryEffectImpl {

  override def addSideEffects(newSideEffects: Iterable[SideEffect]): SecondaryEffectImpl =
    copy(sideEffects = sideEffects ++ newSideEffects)
}

final case class ErrorReplyImpl[T](description: String, sideEffects: Vector[SideEffect]) extends SecondaryEffectImpl {
  override def addSideEffects(newSideEffects: Iterable[SideEffect]): SecondaryEffectImpl =
    copy(sideEffects = sideEffects ++ newSideEffects)
}

final case class NoReply[T](sideEffects: Vector[SideEffect]) extends SecondaryEffectImpl {
  override def addSideEffects(newSideEffects: Iterable[SideEffect]): SecondaryEffectImpl =
    copy(sideEffects = sideEffects ++ newSideEffects)
}

object NoReply {
  private val instance = NoReply[Any](Vector.empty)
  def apply[T]: NoReply[T] = instance.asInstanceOf[NoReply[T]]
}

final case class SideEffectImpl(call: DeferredCall[_, _], synchronous: Boolean) extends SideEffect
