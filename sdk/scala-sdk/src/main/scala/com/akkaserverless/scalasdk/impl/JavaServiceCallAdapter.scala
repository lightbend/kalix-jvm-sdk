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

import com.akkaserverless.scalasdk.SideEffect
import com.akkaserverless.scalasdk.ServiceCall
import com.akkaserverless.scalasdk.ServiceCallRef
import com.akkaserverless.scalasdk.impl.MetadataImpl
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk.Metadata
import com.google.protobuf
import com.google.protobuf.Descriptors

private[scalasdk] case class JavaServiceCallAdapter(scalaSdk: ServiceCall) extends javasdk.ServiceCall {
  override def ref(): javasdk.ServiceCallRef[_] = JavaServiceCallRefAdapter(scalaSdk.ref)
  override def message(): protobuf.Any = scalaSdk.message
  override def metadata(): Metadata = scalaSdk.metadata.asInstanceOf[MetadataImpl].impl
}

private[scalasdk] case class JavaSideEffectAdapter(scalaSdk: SideEffect) extends javasdk.SideEffect {
  override def serviceCall(): javasdk.ServiceCall = JavaServiceCallAdapter(scalaSdk.serviceCall)
  override def synchronous(): Boolean = scalaSdk.synchronous
}

private[scalasdk] case class JavaServiceCallRefAdapter[T](scalasdkServiceCallRef: ServiceCallRef[T])
    extends javasdk.ServiceCallRef[T] {
  def method(): Descriptors.MethodDescriptor = scalasdkServiceCallRef.method

  def createCall(message: T, metadata: javasdk.Metadata): javasdk.ServiceCall = {
    JavaServiceCallAdapter(
      scalasdkServiceCallRef
        .createCall(message, new MetadataImpl(metadata.asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])))
  }
}
