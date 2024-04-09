/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

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

}
