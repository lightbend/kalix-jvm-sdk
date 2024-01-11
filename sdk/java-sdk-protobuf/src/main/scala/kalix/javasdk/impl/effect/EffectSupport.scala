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

import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.SideEffect
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.RestDeferredCall
import kalix.protocol.component

object EffectSupport {

  def asProtocol(messageReply: MessageReplyImpl[JavaPbAny]): component.Reply =
    component.Reply(
      Some(ScalaPbAny.fromJavaProto(messageReply.message)),
      MetadataImpl.toProtocol(messageReply.metadata))

  def asProtocol(messageCodec: MessageCodec, forward: ForwardReplyImpl[_]): component.Forward = {
    forward match {
      case ForwardReplyImpl(deferredCall: GrpcDeferredCall[_, _], sideEffects) =>
        component.Forward(
          deferredCall.fullServiceName,
          deferredCall.methodName,
          Some(messageCodec.encodeScala(forward.deferredCall.message)),
          MetadataImpl.toProtocol(forward.deferredCall.metadata))
      case _ =>
        throw new IllegalArgumentException(s"Unsupported type of deferred call: ${forward.deferredCall.getClass}")
    }

  }

  def asProtocol(messageCodec: MessageCodec, sideEffect: SideEffect): component.SideEffect = {
    sideEffect match {
      case SideEffectImpl(deferred: GrpcDeferredCall[_, _], synchronous) =>
        component.SideEffect(
          deferred.fullServiceName,
          deferred.methodName,
          Some(messageCodec.encodeScala(deferred.message)),
          synchronous,
          MetadataImpl.toProtocol(deferred.metadata))
      case SideEffectImpl(deferred: RestDeferredCall[_, _], synchronous) =>
        component.SideEffect(
          deferred.fullServiceName,
          deferred.methodName,
          Some(messageCodec.encodeScala(deferred.message)),
          synchronous,
          MetadataImpl.toProtocol(deferred.metadata))
    }
  }

  def sideEffectsFrom(
      messageCodec: MessageCodec,
      secondaryEffect: SecondaryEffectImpl): Vector[component.SideEffect] = {
    secondaryEffect.sideEffects.map(asProtocol(messageCodec, _))
  }

}
