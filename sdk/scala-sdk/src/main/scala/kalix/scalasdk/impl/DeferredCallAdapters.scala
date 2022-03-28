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

package com.akkaserverless.scalasdk.impl

import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.DeferredCall
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.SideEffect

import java.util.concurrent.CompletionStage
import scala.jdk.FutureConverters._
import scala.concurrent.Future

/**
 * INTERNAL API
 */
object ScalaDeferredCallAdapter {
  // cannot be package private because used from generated code

  def apply[I, O](
      message: I,
      metadata: Metadata,
      fullServiceName: String,
      methodName: String,
      asyncCall: () => Future[O]): ScalaDeferredCallAdapter[I, O] = ScalaDeferredCallAdapter(
    javasdk.impl.DeferredCallImpl(message, metadata.impl, fullServiceName, methodName, () => asyncCall().asJava))

}

private[scalasdk] final case class ScalaDeferredCallAdapter[I, O](javaSdkDeferredCall: javasdk.DeferredCall[I, O])
    extends DeferredCall[I, O] {
  override def message: I = javaSdkDeferredCall.message
  override def metadata: Metadata =
    new MetadataImpl(javaSdkDeferredCall.metadata.asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])

  def execute(): Future[O] = javaSdkDeferredCall.execute().asScala
}

private[scalasdk] object ScalaSideEffectAdapter {
  def apply(deferredCall: DeferredCall[_, _], synchronous: Boolean): ScalaSideEffectAdapter =
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        ScalaSideEffectAdapter(javasdk.SideEffect.of(javaSdkDeferredCall, synchronous))
    }

  def apply(deferredCall: DeferredCall[_, _]): ScalaSideEffectAdapter =
    deferredCall match {
      case ScalaDeferredCallAdapter(javaSdkDeferredCall) =>
        ScalaSideEffectAdapter(javasdk.SideEffect.of(javaSdkDeferredCall))
    }

}

private[scalasdk] final case class ScalaSideEffectAdapter(javasdkSideEffect: javasdk.SideEffect) extends SideEffect {
  override def serviceCall: DeferredCall[_, _] = ScalaDeferredCallAdapter(javasdkSideEffect.call())
  override def synchronous: Boolean = javasdkSideEffect.synchronous()
}
