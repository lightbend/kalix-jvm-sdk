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

package com.akkaserverless.javasdk.testkit.impl

import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.ServiceCall
import com.akkaserverless.javasdk.ServiceCallFactory
import com.akkaserverless.javasdk.ServiceCallRef
import com.akkaserverless.javasdk.testkit.ServiceCallDetails
import com.google.protobuf
import com.google.protobuf.Descriptors

/**
 * INTERNAL API
 */
object TestKitServiceCallFactory extends ServiceCallFactory {

  private class TestKitServiceCallRef[T](val serviceName: String, val methodName: String, messageType: Class[T])
      extends ServiceCallRef[T] {
    // never expected to be called while unittesting
    override def method(): Descriptors.MethodDescriptor =
      throw new UnsupportedOperationException("Not supported by the testkit")

    override def createCall(message: T, metadata: Metadata): ServiceCall =
      new TestKitServiceCall[T](this, message, metadata)
  }

  final class TestKitServiceCall[T](ref: TestKitServiceCallRef[T], message: T, metadata: Metadata)
      extends ServiceCall
      with ServiceCallDetails[T] {

    // public API for inspection
    override def getServiceName: String = ref.serviceName
    override def getMethodName: String = ref.methodName
    override def getMessage: T = message
    override def getMetadata: Metadata = metadata

    override def ref(): ServiceCallRef[_] = ref
    // never expected to be called while unittesting
    override def message(): protobuf.Any =
      throw new UnsupportedOperationException("Not supported by the testkit")
    // never expected to be called while unittesting
    override def metadata(): Metadata =
      throw new UnsupportedOperationException("Not supported by the testkit")
  }

  override def lookup[T](serviceName: String, methodName: String, messageType: Class[T]): ServiceCallRef[T] =
    new TestKitServiceCallRef[T](serviceName, methodName, messageType)

}
