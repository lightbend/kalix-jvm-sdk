/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.replicatedentity
import kalix.javasdk.impl.replicatedentity.{ ReplicatedEntityEffectImpl => JavaSdkReplicatedEntityEffectImpl }
import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.ScalaSideEffectAdapter
import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntity.Effect
import io.grpc.Status

import scala.jdk.CollectionConverters.IterableHasAsJava

private[scalasdk] object ReplicatedEntityEffectImpl {
  def apply[D <: ReplicatedData, R](): ReplicatedEntityEffectImpl[D, R] =
    ReplicatedEntityEffectImpl(new JavaSdkReplicatedEntityEffectImpl[D, R])
}
private[scalasdk] final case class ReplicatedEntityEffectImpl[D <: ReplicatedData, R](
    javaSdkEffect: JavaSdkReplicatedEntityEffectImpl[D, R])
    extends ReplicatedEntity.Effect.Builder[D]
    with ReplicatedEntity.Effect.OnSuccessBuilder
    with ReplicatedEntity.Effect[R] {

  override def update(newData: D): Effect.OnSuccessBuilder =
    ReplicatedEntityEffectImpl(javaSdkEffect.update(newData))

  override def delete: Effect.OnSuccessBuilder =
    ReplicatedEntityEffectImpl(javaSdkEffect.delete())

  override def reply[T](message: T): ReplicatedEntity.Effect[T] =
    ReplicatedEntityEffectImpl(javaSdkEffect.reply(message))

  override def reply[T](message: T, metadata: Metadata): ReplicatedEntity.Effect[T] =
    ReplicatedEntityEffectImpl(javaSdkEffect.reply(message, MetadataConverters.toJava(metadata)))

  override def forward[T](deferredCall: DeferredCall[_, T]): ReplicatedEntity.Effect[T] =
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        ReplicatedEntityEffectImpl(javaSdkEffect.forward(javaSdkDeferredCall))
    }

  def error[T](description: String): ReplicatedEntity.Effect[T] =
    ReplicatedEntityEffectImpl(javaSdkEffect.error(description))

  def error[T](description: String, statusCode: Status.Code): ReplicatedEntity.Effect[T] =
    ReplicatedEntityEffectImpl(javaSdkEffect.error(description, statusCode))

  override def thenReply[T](message: T): ReplicatedEntity.Effect[T] =
    ReplicatedEntityEffectImpl(javaSdkEffect.thenReply(message))

  override def thenReply[T](message: T, metadata: Metadata): ReplicatedEntity.Effect[T] =
    ReplicatedEntityEffectImpl(javaSdkEffect.thenReply(message, MetadataConverters.toJava(metadata)))

  override def thenForward[T](deferredCall: DeferredCall[_, T]): ReplicatedEntity.Effect[T] =
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        ReplicatedEntityEffectImpl(javaSdkEffect.thenForward(javaSdkDeferredCall))
    }

  override def addSideEffects(sideEffects: Seq[SideEffect]): ReplicatedEntity.Effect[R] = {
    val javaSideEffects =
      sideEffects.map { case ScalaSideEffectAdapter(javasdkSideEffect) => javasdkSideEffect }.asJavaCollection

    ReplicatedEntityEffectImpl(javaSdkEffect.addSideEffects(javaSideEffects))
  }
}
