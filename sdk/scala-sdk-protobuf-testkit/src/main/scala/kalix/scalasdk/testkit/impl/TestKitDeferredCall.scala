/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
