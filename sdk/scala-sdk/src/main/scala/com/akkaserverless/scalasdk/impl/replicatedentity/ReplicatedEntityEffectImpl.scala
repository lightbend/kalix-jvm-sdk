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

package com.akkaserverless.scalasdk.impl.replicatedentity
import com.akkaserverless.javasdk.impl.replicatedentity.{
  ReplicatedEntityEffectImpl => JavaSdkReplicatedEntityEffectImpl
}
import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.DeferredCall
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.SideEffect
import com.akkaserverless.scalasdk.impl.MetadataConverters
import com.akkaserverless.scalasdk.impl.ScalaDeferredCallAdapter
import com.akkaserverless.scalasdk.impl.ScalaSideEffectAdapter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity.Effect

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

  override def error[T](description: String): ReplicatedEntity.Effect[T] =
    ReplicatedEntityEffectImpl(javaSdkEffect.error(description))

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
