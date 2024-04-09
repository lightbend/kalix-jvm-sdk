/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import kalix.scalasdk.Metadata
import kalix.scalasdk.action.ActionContext
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.testkit.MockRegistry

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitActionContext(
    override val metadata: Metadata = Metadata.empty,
    mockRegistry: MockRegistry = MockRegistry.empty)
    extends AbstractTestKitContext(mockRegistry)
    with ActionContext
    with ActionCreationContext {

  override def eventSubject = metadata.get("ce-subject")
  override def getGrpcClient[T](clientClass: Class[T], service: String): T = getComponentGrpcClient(clientClass)
  def getOpenTelemetryTracer: Option[Tracer] = None
  override def getTracer: Tracer = OpenTelemetry.noop().getTracer("noop")
}
