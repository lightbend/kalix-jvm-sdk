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

package kalix.scalasdk.impl

import kalix.javasdk
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.Metadata
import kalix.scalasdk.SideEffect
import scala.concurrent.Future
import scala.jdk.FutureConverters._

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
      asyncCall: Metadata => Future[O]): ScalaDeferredCallAdapter[I, O] = ScalaDeferredCallAdapter(
    javasdk.impl.GrpcDeferredCall(
      message,
      metadata.impl,
      fullServiceName,
      methodName,
      (javaMetadata: javasdk.Metadata) =>
        asyncCall(MetadataImpl(javaMetadata.asInstanceOf[kalix.javasdk.impl.MetadataImpl])).asJava))

}

private[scalasdk] final case class ScalaDeferredCallAdapter[I, O](javaSdkDeferredCall: javasdk.DeferredCall[I, O])
    extends DeferredCall[I, O] {
  override def message: I = javaSdkDeferredCall.message
  override def metadata: Metadata =
    MetadataImpl(javaSdkDeferredCall.metadata.asInstanceOf[kalix.javasdk.impl.MetadataImpl])

  def execute(): Future[O] = javaSdkDeferredCall.execute().asScala

  override def withMetadata(metadata: Metadata): ScalaDeferredCallAdapter[I, O] = {
    ScalaDeferredCallAdapter(javaSdkDeferredCall.withMetadata(metadata.impl))
  }
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
