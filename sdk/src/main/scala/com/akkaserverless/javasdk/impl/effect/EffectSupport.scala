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

import com.akkaserverless.javasdk
import com.akkaserverless.javasdk.impl.MetadataImpl
import com.akkaserverless.protocol.component
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{Any => JavaPbAny}

object EffectSupport {
  private def asProtocol(metadata: javasdk.Metadata): Option[component.Metadata] =
    metadata match {
      case impl: MetadataImpl if impl.entries.nonEmpty =>
        Some(component.Metadata(impl.entries))
      case _: MetadataImpl => None
      case other =>
        throw new RuntimeException(s"Unknown metadata implementation: ${other.getClass}, cannot send")
    }

  def asProtocol(messageReply: MessageReplyImpl[JavaPbAny]): component.Reply =
    component.Reply(
      Some(ScalaPbAny.fromJavaProto(messageReply.message)),
      asProtocol(messageReply.metadata)
    )

  def asProtocol(forward: ForwardReplyImpl[_]): component.Forward =
    component.Forward(
      forward.serviceCall.ref().method().getService.getFullName,
      forward.serviceCall.ref().method().getName,
      Some(ScalaPbAny.fromJavaProto(forward.serviceCall.message())),
      asProtocol(forward.serviceCall.metadata())
    )

  def sideEffectsFrom(secondaryEffect: SecondaryEffectImpl): Vector[component.SideEffect] = {
    val encodedSideEffects = secondaryEffect.sideEffects.map { sideEffect =>
      component.SideEffect(
        sideEffect.serviceCall().ref().method().getService.getFullName,
        sideEffect.serviceCall().ref().method().getName,
        Some(ScalaPbAny.fromJavaProto(sideEffect.serviceCall().message())),
        sideEffect.synchronous(),
        asProtocol(sideEffect.serviceCall().metadata())
      )
    }
    encodedSideEffects
  }

}
