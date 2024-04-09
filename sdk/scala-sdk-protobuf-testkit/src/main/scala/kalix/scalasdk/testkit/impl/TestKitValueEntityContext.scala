/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import akka.stream.Materializer
import kalix.scalasdk.testkit.MockRegistry
import kalix.scalasdk.valueentity.ValueEntityContext

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitValueEntityContext(override val entityId: String, mockRegistry: MockRegistry = MockRegistry.empty)
    extends AbstractTestKitContext(mockRegistry)
    with ValueEntityContext {
  override def materializer(): Materializer = throw new UnsupportedOperationException(
    "Accessing the materializer from testkit not supported yet")
}
