/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl

import kalix.javasdk.Metadata
import kalix.javasdk.testkit.MockRegistry
import kalix.javasdk.valueentity.{ CommandContext, ValueEntityContext }

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitValueEntityCommandContext(
    override val entityId: String,
    override val commandName: String = "stubCommandName",
    override val commandId: Long = 0L,
    override val metadata: Metadata = Metadata.EMPTY,
    mockRegistry: MockRegistry = MockRegistry.EMPTY)
    extends AbstractTestKitContext(mockRegistry)
    with ValueEntityContext
    with CommandContext {

  def this(entityId: String, metadata: Metadata) = {
    this(entityId = entityId, metadata = metadata, commandName = "stubCommandName")
  }

}
