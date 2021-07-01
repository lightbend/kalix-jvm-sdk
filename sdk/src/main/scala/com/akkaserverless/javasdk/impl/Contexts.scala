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

package com.akkaserverless.javasdk.impl

import com.akkaserverless.javasdk
import com.akkaserverless.javasdk.impl.reply.{NoReply, ReplySupport}
import com.akkaserverless.javasdk.{ClientActionContext, Context, ServiceCall, SideEffectContext}
import com.akkaserverless.javasdk.reply._
import com.akkaserverless.protocol.component._
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{Any => JavaPbAny}
import scala.util.control.NoStackTrace

import com.akkaserverless.javasdk.impl.effect.EffectSupport
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl

private[impl] trait ActivatableContext extends Context {
  private final var active = true
  final def deactivate(): Unit = active = false
  final def checkActive(): Unit = if (!active) throw new IllegalStateException("Context no longer active!")
}

private[impl] trait AbstractSideEffectContext extends SideEffectContext {
  self: ActivatableContext =>

  private final var _sideEffects = List.empty[SideEffect]

  override final def effect(effect: ServiceCall, synchronous: Boolean): Unit = {
    checkActive()
    _sideEffects = SideEffect(
        serviceName = effect.ref().method().getService.getFullName,
        commandName = effect.ref().method().getName,
        payload = Some(ScalaPbAny.fromJavaProto(effect.message())),
        synchronous = synchronous
      ) :: _sideEffects
  }

  final def sideEffects: List[SideEffect] = _sideEffects.reverse
}

private[impl] trait AbstractClientActionContext extends ClientActionContext {
  self: ActivatableContext =>

  def commandId: Long

  private final var error: Option[String] = None
  private final var forward: Option[Forward] = None

  override final def fail(errorMessage: String): RuntimeException = {
    checkActive()
    if (error.isEmpty) {
      error = Some(errorMessage)
      logError(errorMessage)
      throw FailInvoked
    } else throw new IllegalStateException("fail(…) already previously invoked!")
  }

  @Deprecated
  override final def forward(to: ServiceCall): Unit = {
    checkActive()
    if (forward.isDefined) {
      throw new IllegalStateException("This context has already forwarded.")
    }
    forward = Some(
      Forward(
        serviceName = to.ref().method().getService.getFullName,
        commandName = to.ref().method().getName,
        payload = Some(ScalaPbAny.fromJavaProto(to.message()))
      )
    )
  }

  final def hasError: Boolean = error.isDefined

  protected def logError(message: String): Unit = ()

  final def replyToClientAction(msg: javasdk.Reply[JavaPbAny],
                                allowNoReply: Boolean,
                                restartOnFailure: Boolean): Option[ClientAction] =
    error match {
      case Some(msg) => Some(ClientAction(ClientAction.Action.Failure(Failure(commandId, msg, restartOnFailure))))
      case None =>
        msg match {
          case message: MessageReply[JavaPbAny] =>
            if (forward.isDefined) {
              throw new IllegalStateException(
                "Both a reply was returned, and a forward message was sent, choose one or the other."
              )
            }
            Some(ClientAction(ClientAction.Action.Reply(ReplySupport.asProtocol(message))))
          case forward: ForwardReply[JavaPbAny] =>
            Some(ClientAction(ClientAction.Action.Forward(ReplySupport.asProtocol(forward))))
          case failure: ErrorReply[JavaPbAny] =>
            Some(ClientAction(ClientAction.Action.Failure(Failure(commandId, failure.description(), restartOnFailure))))
          case _: NoReply[_] =>
            if (forward.isDefined) {
              Some(ClientAction(ClientAction.Action.Forward(forward.get)))
            } else if (allowNoReply) {
              None
            } else {
              throw new RuntimeException("No reply or forward returned by command handler!")
            }
        }
    }

  final def replyToClientAction(secondaryEffect: SecondaryEffectImpl,
                                allowNoReply: Boolean,
                                restartOnFailure: Boolean): Option[ClientAction] = {
    error match {
      case Some(msg) => Some(ClientAction(ClientAction.Action.Failure(Failure(commandId, msg, restartOnFailure))))
      case None =>
        secondaryEffect match {
          case message: effect.MessageReplyImpl[JavaPbAny] @unchecked =>
            if (forward.isDefined) {
              throw new IllegalStateException(
                "Both a reply was returned, and a forward message was sent, choose one or the other."
              )
            }
            Some(ClientAction(ClientAction.Action.Reply(EffectSupport.asProtocol(message))))
          case forward: effect.ForwardReplyImpl[JavaPbAny] @unchecked =>
            Some(ClientAction(ClientAction.Action.Forward(EffectSupport.asProtocol(forward))))
          case failure: effect.ErrorReplyImpl[JavaPbAny] @unchecked =>
            Some(ClientAction(ClientAction.Action.Failure(Failure(commandId, failure.description, restartOnFailure))))
          case _: effect.NoReply[_] @unchecked | effect.NoSecondaryEffectImpl =>
            if (forward.isDefined) {
              Some(ClientAction(ClientAction.Action.Forward(forward.get)))
            } else if (allowNoReply) {
              None
            } else {
              throw new RuntimeException("No reply or forward returned by command handler!")
            }
        }
    }
  }

}

object FailInvoked extends RuntimeException with NoStackTrace {
  override def toString: String = "CommandContext.fail(…) invoked"
}
