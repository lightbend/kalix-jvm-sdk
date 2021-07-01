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

import com.akkaserverless.javasdk
import com.akkaserverless.javasdk.impl.MetadataImpl
import com.akkaserverless.javasdk.reply.{ForwardReply, MessageReply}
import com.akkaserverless.protocol.component
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{GeneratedMessage, Any => JavaPbAny}

import scala.jdk.CollectionConverters._
import com.akkaserverless.javasdk.SideEffect

object ReplySupport {
  private def asProtocol(metadata: javasdk.Metadata): Option[component.Metadata] =
    metadata match {
      case impl: MetadataImpl if impl.entries.nonEmpty =>
        Some(component.Metadata(impl.entries))
      case _: MetadataImpl => None
      case other =>
        throw new RuntimeException(s"Unknown metadata implementation: ${other.getClass}, cannot send")
    }

  def asProtocol(message: MessageReply[JavaPbAny]): component.Reply =
    component.Reply(
      Some(ScalaPbAny.fromJavaProto(message.payload())),
      asProtocol(message.metadata())
    )

  def asProtocol(forward: ForwardReply[_]): component.Forward =
    component.Forward(
      forward.serviceCall().ref().method().getService.getFullName,
      forward.serviceCall().ref().method().getName,
      Some(ScalaPbAny.fromJavaProto(forward.serviceCall().message())),
      asProtocol(forward.serviceCall().metadata())
    )

  def effectsFrom(reply: javasdk.Reply[JavaPbAny]): List[component.SideEffect] = {
    val replyEffects: List[SideEffect] = reply match {
      case impl: ReplyImpl[_] =>
        impl._effects
      case other =>
        other.sideEffects().asScala.toList
    }

    val encodedEffects = replyEffects.map { effect =>
      component.SideEffect(
        effect.serviceCall().ref().method().getService.getFullName,
        effect.serviceCall().ref().method().getName,
        Some(ScalaPbAny.fromJavaProto(effect.serviceCall().message())),
        effect.synchronous(),
        asProtocol(effect.serviceCall().metadata())
      )
    }
    encodedEffects
  }
}
