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

package kalix.javasdk.impl.effect

import com.google.protobuf.{ Any => JavaPbAny }
import io.grpc.Status
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.impl.effect
import kalix.javasdk.DeferredCall
import kalix.javasdk.Metadata
import kalix.javasdk.SideEffect
import kalix.protocol.component.ClientAction

sealed trait SecondaryEffectImpl {
  def sideEffects: Vector[SideEffect]
  def addSideEffects(sideEffects: Iterable[SideEffect]): SecondaryEffectImpl

  final def replyToClientAction(messageCodec: MessageCodec, commandId: Long): Option[ClientAction] = {
    this match {
      case message: effect.MessageReplyImpl[JavaPbAny] @unchecked =>
        Some(ClientAction(ClientAction.Action.Reply(EffectSupport.asProtocol(message))))
      case forward: effect.ForwardReplyImpl[JavaPbAny] @unchecked =>
        Some(ClientAction(ClientAction.Action.Forward(EffectSupport.asProtocol(messageCodec, forward))))
      case failure: effect.ErrorReplyImpl[JavaPbAny] @unchecked =>
        Some(
          ClientAction(
            ClientAction.Action
              .Failure(kalix.protocol.component
                .Failure(commandId, failure.description, grpcStatusCode = failure.status.map(_.value()).getOrElse(0)))))
      case NoSecondaryEffectImpl(_) =>
        throw new RuntimeException("No reply or forward returned by command handler!")
    }
  }
}

case class NoSecondaryEffectImpl(sideEffects: Vector[SideEffect] = Vector.empty) extends SecondaryEffectImpl {

  override def addSideEffects(newSideEffects: Iterable[SideEffect]): SecondaryEffectImpl =
    copy(sideEffects = sideEffects ++ newSideEffects)
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

final case class ErrorReplyImpl[T](description: String, status: Option[Status.Code], sideEffects: Vector[SideEffect])
    extends SecondaryEffectImpl {
  override def addSideEffects(newSideEffects: Iterable[SideEffect]): SecondaryEffectImpl =
    copy(sideEffects = sideEffects ++ newSideEffects)
}

final case class SideEffectImpl(call: DeferredCall[_, _], synchronous: Boolean) extends SideEffect
