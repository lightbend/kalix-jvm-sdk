/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.replicatedentity

import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.replicatedentity.CommandContext

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ReplicatedEntityRouter[D <: ReplicatedData, E <: ReplicatedEntity[D]](val entity: E) {

  def handleCommand(commandName: String, data: D, command: Any, context: CommandContext): ReplicatedEntity.Effect[_]
}
