/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import kalix.scalasdk.Metadata
import kalix.scalasdk.valueentity.CommandContext
import kalix.scalasdk.valueentity.ValueEntityContext

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitValueEntityCommandContext(
    override val entityId: String,
    override val commandName: String = "stubCommandName",
    override val commandId: Long = 0L,
    override val metadata: Metadata = Metadata.empty)
    extends AbstractTestKitContext
    with ValueEntityContext
    with CommandContext {}
