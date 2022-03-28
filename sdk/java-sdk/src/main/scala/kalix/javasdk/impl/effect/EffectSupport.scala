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
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.impl.DeferredCallImpl
import com.akkaserverless.javasdk.impl.MetadataImpl
import com.akkaserverless.protocol.component
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }

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
    component.Reply(Some(ScalaPbAny.fromJavaProto(messageReply.message)), asProtocol(messageReply.metadata))

  def asProtocol(anySupport: AnySupport, forward: ForwardReplyImpl[_]): component.Forward = {
    forward match {
      case ForwardReplyImpl(deferredCall: DeferredCallImpl[_, _], sideEffects) =>
        component.Forward(
          deferredCall.fullServiceName,
          deferredCall.methodName,
          Some(ScalaPbAny.fromJavaProto(anySupport.encodeJava(forward.deferredCall.message))),
          asProtocol(forward.deferredCall.metadata))
      case _ =>
        throw new IllegalArgumentException(s"Unsupported type of deferred call: ${forward.deferredCall.getClass}")
    }

  }

  def sideEffectsFrom(anySupport: AnySupport, secondaryEffect: SecondaryEffectImpl): Vector[component.SideEffect] = {
    val encodedSideEffects = secondaryEffect.sideEffects.map {
      case SideEffectImpl(deferred: DeferredCallImpl[_, _], synchronous) =>
        component.SideEffect(
          deferred.fullServiceName,
          deferred.methodName,
          Some(ScalaPbAny.fromJavaProto(anySupport.encodeJava(deferred.message))),
          synchronous,
          asProtocol(deferred.metadata))
    }
    encodedSideEffects
  }

}
