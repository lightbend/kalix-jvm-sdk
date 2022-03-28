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

package com.akkaserverless.scalasdk.impl.valueentity

import scala.jdk.CollectionConverters._
import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.SideEffect
import com.akkaserverless.scalasdk.impl.ScalaDeferredCallAdapter
import com.akkaserverless.scalasdk.impl.ScalaSideEffectAdapter
import com.akkaserverless.scalasdk.valueentity.ValueEntity
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

  def deleteState: ValueEntity.Effect.OnSuccessBuilder[S] = new ValueEntityEffectImpl(javasdkEffect.deleteState())

  def error[T](description: String): ValueEntity.Effect[T] = new ValueEntityEffectImpl(
    javasdkEffect.error[T](description))

  def error[T](description: String, statusCode: Option[Status.Code]): ValueEntity.Effect[T] =
    ValueEntityEffectImpl(statusCode match {
      case Some(code) => javasdkEffect.error(description, code)
      case None       => javasdkEffect.error(description)
    })

  def forward[T](deferredCall: com.akkaserverless.scalasdk.DeferredCall[_, T]): ValueEntity.Effect[T] = {
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        new ValueEntityEffectImpl(javasdkEffect.forward(javaSdkDeferredCall))
    }
  }

  def noReply[T]: ValueEntity.Effect[T] = new ValueEntityEffectImpl(javasdkEffect.noReply[T]())

  def reply[T](message: T, metadata: com.akkaserverless.scalasdk.Metadata): ValueEntity.Effect[T] =
    ValueEntityEffectImpl(javasdkEffect.reply(message, metadata.impl))

  def reply[T](message: T): ValueEntity.Effect[T] = new ValueEntityEffectImpl(javasdkEffect.reply(message))

  def updateState(newState: S): ValueEntity.Effect.OnSuccessBuilder[S] = new ValueEntityEffectImpl(
    javasdkEffect.updateState(newState))

  def addSideEffects(sideEffects: Seq[SideEffect]): ValueEntity.Effect[S] = new ValueEntityEffectImpl(
    javasdkEffect
      .addSideEffects(sideEffects.map { case ScalaSideEffectAdapter(javasdkSideEffect) =>
        javasdkSideEffect
      }.asJavaCollection))

  def thenForward[T](deferredCall: com.akkaserverless.scalasdk.DeferredCall[_, T]): ValueEntity.Effect[T] = {
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        ValueEntityEffectImpl(javasdkEffect.thenForward(javaSdkDeferredCall))
    }
  }

  def thenNoReply[T]: ValueEntity.Effect[T] = new ValueEntityEffectImpl(javasdkEffect.thenNoReply())

  def thenReply[T](message: T, metadata: com.akkaserverless.scalasdk.Metadata): ValueEntity.Effect[T] =
    ValueEntityEffectImpl(javasdkEffect.thenReply(message, metadata.impl))

  def thenReply[T](message: T): ValueEntity.Effect[T] = new ValueEntityEffectImpl(javasdkEffect.thenReply(message))

}
