/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import akka.stream.Materializer
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitEventSourcedEntityContext(override val entityId: String)
    extends AbstractTestKitContext
    with EventSourcedEntityContext {
  override def materializer(): Materializer = throw new UnsupportedOperationException(
    "Accessing the materializer from testkit not supported yet")
}
