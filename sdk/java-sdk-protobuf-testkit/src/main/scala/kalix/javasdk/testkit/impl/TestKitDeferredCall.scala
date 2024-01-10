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

package kalix.javasdk.testkit.impl

import kalix.javasdk.Metadata
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.testkit.DeferredCallDetails

final case class TestKitDeferredCall[I, O](deferredCall: GrpcDeferredCall[I, O]) extends DeferredCallDetails[I, O] {
  // public API for inspection
  override def getServiceName: String = deferredCall.fullServiceName
  override def getMethodName: String = deferredCall.methodName
  override def getMessage: I = deferredCall.message
  override def getMetadata: Metadata = deferredCall.metadata
}
