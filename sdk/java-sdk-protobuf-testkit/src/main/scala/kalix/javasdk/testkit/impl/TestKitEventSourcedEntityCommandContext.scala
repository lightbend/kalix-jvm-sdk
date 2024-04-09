/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl

import kalix.javasdk.eventsourcedentity.CommandContext
import kalix.javasdk.Metadata
import kalix.javasdk.impl.InternalContext
import akka.stream.Materializer

/** INTERNAL API Used by the generated testkit */
final class TestKitEventSourcedEntityCommandContext(
    override val entityId: String = "stubEntityId",
    override val commandId: Long = 0L,
    override val commandName: String = "stubCommandName",
    override val sequenceNumber: Long = 0L,
    override val metadata: Metadata = Metadata.EMPTY)
    extends CommandContext
    with InternalContext {

  def this(metadata: Metadata) = {
    this(metadata = metadata, commandName = "stubCommandName")
  }

  override def materializer(): Materializer = throw new UnsupportedOperationException(
    "Accessing the materializer from testkit not supported yet")
  override def getComponentGrpcClient[T](serviceClass: Class[T]): T = throw new UnsupportedOperationException(
    "Accessing the componentGrpcClient from testkit not supported yet")

}

object TestKitEventSourcedEntityCommandContext {
  def empty = new TestKitEventSourcedEntityCommandContext()
}
