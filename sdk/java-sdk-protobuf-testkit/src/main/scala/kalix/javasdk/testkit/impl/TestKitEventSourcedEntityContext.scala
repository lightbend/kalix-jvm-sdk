/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl

import akka.stream.Materializer
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext
import kalix.javasdk.testkit.MockRegistry

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitEventSourcedEntityContext(
    override val entityId: String,
    mockRegistry: MockRegistry = MockRegistry.EMPTY)
    extends AbstractTestKitContext(mockRegistry)
    with EventSourcedEntityContext {

  def this(entityId: String) = {
    this(entityId, MockRegistry.EMPTY)
  }

  override def materializer(): Materializer = throw new UnsupportedOperationException(
    "Accessing the materializer from testkit not supported yet")
}
