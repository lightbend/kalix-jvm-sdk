/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import kalix.scalasdk.eventsourcedentity.CommandContext
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.InternalContext
import akka.stream.Materializer

/** INTERNAL API Used by the generated testkit */
final class TestKitEventSourcedEntityCommandContext(
    override val entityId: String = "stubEntityId",
    override val commandId: Long = 0L,
    override val commandName: String = "stubCommandName",
    override val sequenceNumber: Long = 0L,
    override val metadata: Metadata = Metadata.empty)
    extends CommandContext
    with InternalContext {
  override def materializer(): Materializer = throw new UnsupportedOperationException(
    "Accessing the materializer from testkit not supported yet")
  override def getComponentGrpcClient[T](serviceClass: Class[T]): T = throw new UnsupportedOperationException(
    "Accessing the componentGrpcClient from testkit not supported yet")
}
