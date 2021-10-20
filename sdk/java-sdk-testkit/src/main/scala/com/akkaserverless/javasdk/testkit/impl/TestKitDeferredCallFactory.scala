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

import com.akkaserverless.javasdk.DeferredCall
import com.akkaserverless.javasdk.DeferredCallFactory
import com.akkaserverless.javasdk.DeferredCallRef
import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.testkit.DeferredCallDetails
import com.google.protobuf
import com.google.protobuf.Descriptors

import java.util.concurrent.CompletionStage

/**
 * INTERNAL API
 */
object TestKitDeferredCallFactory extends DeferredCallFactory {

  private class TestKitServiceCallRef[T, R](val serviceName: String, val methodName: String, messageType: Class[T])
      extends DeferredCallRef[T, R] {
    // never expected to be called while unittesting
    override def method(): Descriptors.MethodDescriptor =
      throw new UnsupportedOperationException("Not supported by the testkit")

    override def createCall(message: T, metadata: Metadata): DeferredCall[T, R] =
      new TestKitDeferredCall[T, R](this, message, metadata)
  }

  final class TestKitDeferredCall[T, R](ref: TestKitServiceCallRef[T, R], message: T, metadata: Metadata)
      extends DeferredCall[T, R]
      with DeferredCallDetails[T] {

    // public API for inspection
    override def getServiceName: String = ref.serviceName
    override def getMethodName: String = ref.methodName
    override def getMessage: T = message
    override def getMetadata: Metadata = metadata

    override def ref(): DeferredCallRef[T, R] = ref
    override def metadata(): Metadata = this.metadata

    // never expected to be called while unittesting
    override def message(): protobuf.Any =
      throw new UnsupportedOperationException("Not supported by the testkit")
    def execute(): CompletionStage[R] =
      throw new UnsupportedOperationException("Async calls to other components not supported by the testkit")
  }

  // FIXME unsafe, maybe make internal/deprecate?
  override def lookup[T, R](serviceName: String, methodName: String, messageType: Class[T]): DeferredCallRef[T, R] =
    new TestKitServiceCallRef[T, R](serviceName, methodName, messageType)

}
