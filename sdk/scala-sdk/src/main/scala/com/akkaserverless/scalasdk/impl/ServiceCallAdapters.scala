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
import com.akkaserverless.scalasdk
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.ServiceCall
import com.akkaserverless.scalasdk.ServiceCallFactory
import com.akkaserverless.scalasdk.ServiceCallRef
import com.akkaserverless.scalasdk.SideEffect
import com.google.protobuf
import com.google.protobuf.Descriptors

private[scalasdk] case class ScalaServiceCallFactoryAdapter(javaSdkServiceCallFactory: javasdk.ServiceCallFactory)
    extends ServiceCallFactory {

  override def lookup[T](serviceName: String, methodName: String, messageType: Class[T]): ServiceCallRef[T] =
    ScalaServiceCallRefAdapter(javaSdkServiceCallFactory.lookup(serviceName, methodName, messageType))

}
private[scalasdk] case class JavaServiceCallAdapter(scalaSdkServiceCall: ServiceCall) extends javasdk.ServiceCall {
  override def ref(): javasdk.ServiceCallRef[_] = JavaServiceCallRefAdapter(scalaSdkServiceCall.ref)
  override def message(): protobuf.Any = scalaSdkServiceCall.message
  override def metadata(): javasdk.Metadata = scalaSdkServiceCall.metadata.asInstanceOf[MetadataImpl].impl
}

private[scalasdk] case class ScalaServiceCallAdapter(javaSdkServiceCall: javasdk.ServiceCall) extends ServiceCall {
  override def ref: ServiceCallRef[_] = ScalaServiceCallRefAdapter(javaSdkServiceCall.ref())
  override def message: protobuf.Any = javaSdkServiceCall.message
  override def metadata: Metadata =
    new MetadataImpl(javaSdkServiceCall.metadata.asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])
}

private[scalasdk] case class JavaSideEffectAdapter(scalaSdkSideEffect: SideEffect) extends javasdk.SideEffect {
  override def serviceCall(): javasdk.ServiceCall = JavaServiceCallAdapter(scalaSdkSideEffect.serviceCall)
  override def synchronous(): Boolean = scalaSdkSideEffect.synchronous
}

private[scalasdk] case class JavaServiceCallRefAdapter[T](scalaSdkServiceCallRef: ServiceCallRef[T])
    extends javasdk.ServiceCallRef[T] {
  def method(): Descriptors.MethodDescriptor = scalaSdkServiceCallRef.method

  def createCall(message: T, metadata: javasdk.Metadata): javasdk.ServiceCall = {
    JavaServiceCallAdapter(
      scalaSdkServiceCallRef
        .createCall(message, new MetadataImpl(metadata.asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])))
  }
}

private[scalasdk] case class ScalaServiceCallRefAdapter[T](javaSdkServiceCallRef: javasdk.ServiceCallRef[T])
    extends ServiceCallRef[T] {
  override def method: Descriptors.MethodDescriptor = javaSdkServiceCallRef.method()
  override def createCall(message: T, metadata: scalasdk.Metadata): ServiceCall =
    ScalaServiceCallAdapter(javaSdkServiceCallRef.createCall(message, metadata.impl))
}
