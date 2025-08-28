/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import akka.stream.Materializer
import kalix.scalasdk.Metadata
import kalix.scalasdk.eventsourcedentity.EventContext

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitEventSourcedEntityEventContext extends EventContext {
  override def entityId = "testkit-entity-id"
  override def sequenceNumber = 0L
  override def materializer(): Materializer = throw new UnsupportedOperationException(
    "Accessing the materializer from testkit not supported yet")
  override def metadata: Metadata = Metadata.empty
}
