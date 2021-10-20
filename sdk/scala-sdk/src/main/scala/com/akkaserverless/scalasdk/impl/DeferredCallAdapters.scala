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
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.DeferredCall
import com.akkaserverless.scalasdk.DeferredCallFactory
import com.akkaserverless.scalasdk.DeferredCallRef
import com.akkaserverless.scalasdk.SideEffect
import com.google.protobuf
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }

private[scalasdk] final case class ScalaDeferredCallFactoryAdapter(
    javaSdkServiceCallFactory: javasdk.DeferredCallFactory)
    extends DeferredCallFactory {

  override def lookup[T, R](serviceName: String, methodName: String, messageType: Class[T]): DeferredCallRef[T, R] =
    ScalaServiceCallRefAdapter(javaSdkServiceCallFactory.lookup(serviceName, methodName, messageType))

}
private[scalasdk] final case class JavaDeferredCallAdapter[T, R](scalaSdkServiceCall: DeferredCall[T, R])
    extends javasdk.DeferredCall[T, R] {
  override def ref(): javasdk.DeferredCallRef[T, R] = JavaDeferredCallRefAdapter(scalaSdkServiceCall.ref)
  override def message(): protobuf.Any = ScalaPbAny.toJavaProto(scalaSdkServiceCall.message)
  override def metadata(): javasdk.Metadata = scalaSdkServiceCall.metadata.asInstanceOf[MetadataImpl].impl
}

private[scalasdk] final case class ScalaServiceCallAdapter[T, R](javaSdkServiceCall: javasdk.DeferredCall[T, R])
    extends DeferredCall[T, R] {
  override def ref: DeferredCallRef[T, R] = ScalaServiceCallRefAdapter(javaSdkServiceCall.ref())
  override def message: ScalaPbAny = ScalaPbAny.fromJavaProto(javaSdkServiceCall.message)
  override def metadata: Metadata =
    new MetadataImpl(javaSdkServiceCall.metadata.asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])
}

private[scalasdk] final case class JavaSideEffectAdapter(scalaSdkSideEffect: SideEffect) extends javasdk.SideEffect {
  override def call(): javasdk.DeferredCall[_, _] = JavaDeferredCallAdapter(scalaSdkSideEffect.serviceCall)
  override def synchronous(): Boolean = scalaSdkSideEffect.synchronous
}

private[scalasdk] final case class JavaDeferredCallRefAdapter[T, R](scalaSdkServiceCallRef: DeferredCallRef[T, R])
    extends javasdk.DeferredCallRef[T, R] {
  def method(): Descriptors.MethodDescriptor = scalaSdkServiceCallRef.method

  def createCall(message: T, metadata: javasdk.Metadata): javasdk.DeferredCall[T, R] = {
    JavaDeferredCallAdapter(
      scalaSdkServiceCallRef
        .createCall(message, new MetadataImpl(metadata.asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])))
  }
}

private[scalasdk] final case class ScalaServiceCallRefAdapter[T, R](
    javaSdkServiceCallRef: javasdk.DeferredCallRef[T, R])
    extends DeferredCallRef[T, R] {
  override def method: Descriptors.MethodDescriptor = javaSdkServiceCallRef.method()
  override def createCall(message: T, metadata: scalasdk.Metadata): DeferredCall[T, R] =
    ScalaServiceCallAdapter(javaSdkServiceCallRef.createCall(message, metadata.impl))
}
