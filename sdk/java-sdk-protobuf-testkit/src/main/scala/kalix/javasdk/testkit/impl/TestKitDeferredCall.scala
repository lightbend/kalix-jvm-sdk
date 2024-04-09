/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
