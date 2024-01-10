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

package kalix.scalasdk.testkit.impl

import kalix.javasdk.impl.GrpcDeferredCall
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.testkit.DeferredCallDetails

import scala.concurrent.Future

final case class TestKitDeferredCall[I, O](deferredCall: GrpcDeferredCall[I, O]) extends DeferredCallDetails[I, O] {
  // public API for inspection
  override def serviceName: String = deferredCall.fullServiceName
  override def methodName: String = deferredCall.methodName
  override def message: I = deferredCall.message
  override def metadata: Metadata = MetadataConverters.toScala(deferredCall.metadata)
  def execute(): Future[O] =
    throw new UnsupportedOperationException("Async calls to other components not supported by the testkit")

  override def withMetadata(metadata: Metadata): DeferredCall[I, O] = {
    TestKitDeferredCall(deferredCall.withMetadata(metadata.impl))
  }
}
