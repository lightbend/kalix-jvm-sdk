/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
      case ForwardReplyImpl(deferredCall: GrpcDeferredCall[_, _], _) =>
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
