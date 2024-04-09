/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.valueentity

import scala.jdk.CollectionConverters._
import kalix.javasdk
import kalix.scalasdk.SideEffect
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.ScalaSideEffectAdapter
import kalix.scalasdk.valueentity.ValueEntity
import io.grpc.Status

private[scalasdk] object ValueEntityEffectImpl {
  def apply[S](): ValueEntityEffectImpl[S] = ValueEntityEffectImpl(
    new javasdk.impl.valueentity.ValueEntityEffectImpl[S]())
}

private[scalasdk] final case class ValueEntityEffectImpl[S](
    javasdkEffect: javasdk.impl.valueentity.ValueEntityEffectImpl[S])
    extends ValueEntity.Effect.Builder[S]
    with ValueEntity.Effect.OnSuccessBuilder[S]
    with ValueEntity.Effect[S] {

  def deleteEntity(): ValueEntity.Effect.OnSuccessBuilder[S] = new ValueEntityEffectImpl(javasdkEffect.deleteEntity())

  def deleteState(): ValueEntity.Effect.OnSuccessBuilder[S] = deleteEntity()

  def error[T](description: String): ValueEntity.Effect[T] = new ValueEntityEffectImpl(
    javasdkEffect.error[T](description))

  def error[T](description: String, statusCode: Status.Code): ValueEntity.Effect[T] =
    new ValueEntityEffectImpl(javasdkEffect.error[T](description, statusCode))

  def forward[T](deferredCall: kalix.scalasdk.DeferredCall[_, T]): ValueEntity.Effect[T] = {
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        new ValueEntityEffectImpl(javasdkEffect.forward(javaSdkDeferredCall))
    }
  }

  def reply[T](message: T, metadata: kalix.scalasdk.Metadata): ValueEntity.Effect[T] =
    ValueEntityEffectImpl(javasdkEffect.reply(message, metadata.impl))

  def reply[T](message: T): ValueEntity.Effect[T] = new ValueEntityEffectImpl(javasdkEffect.reply(message))

  def updateState(newState: S): ValueEntity.Effect.OnSuccessBuilder[S] = new ValueEntityEffectImpl(
    javasdkEffect.updateState(newState))

  def addSideEffects(sideEffects: Seq[SideEffect]): ValueEntity.Effect[S] = new ValueEntityEffectImpl(
    javasdkEffect
      .addSideEffects(sideEffects.map { case ScalaSideEffectAdapter(javasdkSideEffect) =>
        javasdkSideEffect
      }.asJavaCollection))

  def thenForward[T](deferredCall: kalix.scalasdk.DeferredCall[_, T]): ValueEntity.Effect[T] = {
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        ValueEntityEffectImpl(javasdkEffect.thenForward(javaSdkDeferredCall))
    }
  }

  def thenReply[T](message: T, metadata: kalix.scalasdk.Metadata): ValueEntity.Effect[T] =
    ValueEntityEffectImpl(javasdkEffect.thenReply(message, metadata.impl))

  def thenReply[T](message: T): ValueEntity.Effect[T] = new ValueEntityEffectImpl(javasdkEffect.thenReply(message))

}
