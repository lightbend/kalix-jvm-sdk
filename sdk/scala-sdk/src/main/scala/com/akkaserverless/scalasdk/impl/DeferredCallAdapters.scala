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

// FIXME why are both these two adapters needed?
private[scalasdk] final case class JavaDeferredCallAdapter[I, O](scalaSdkServiceCall: DeferredCall[I, O])
    extends javasdk.DeferredCall[I, O] {
  override def message(): I = scalaSdkServiceCall.message
  override def metadata(): javasdk.Metadata = scalaSdkServiceCall.metadata.asInstanceOf[MetadataImpl].impl

  def execute(): CompletionStage[O] = scalaSdkServiceCall.execute().asJava
}

/**
 * INTERNAL API
 */
// cannot be package private because used from generated code
object ScalaDeferredCallAdapter {

  def apply[I, O](
      message: I,
      metadata: Metadata,
      fullServiceName: String,
      methodName: String,
      asyncCall: () => Future[O]): ScalaDeferredCallAdapter[I, O] = ScalaDeferredCallAdapter(
    javasdk.impl.DeferredCallImpl(message, metadata.impl, fullServiceName, methodName, () => asyncCall().asJava))

}

private[scalasdk] final case class ScalaDeferredCallAdapter[I, O](javaSdkServiceCall: javasdk.DeferredCall[I, O])
    extends DeferredCall[I, O] {
  override def message: I = javaSdkServiceCall.message
  override def metadata: Metadata =
    new MetadataImpl(javaSdkServiceCall.metadata.asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])

  def execute(): Future[O] = javaSdkServiceCall.execute().asScala
}

private[scalasdk] final case class JavaSideEffectAdapter(scalaSdkSideEffect: SideEffect) extends javasdk.SideEffect {
  override def call(): javasdk.DeferredCall[_, _] = JavaDeferredCallAdapter(scalaSdkSideEffect.serviceCall)
  override def synchronous(): Boolean = scalaSdkSideEffect.synchronous
}
