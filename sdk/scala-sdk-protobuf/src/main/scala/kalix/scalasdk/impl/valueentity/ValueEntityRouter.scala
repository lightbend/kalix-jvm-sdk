/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.valueentity

import kalix.scalasdk.valueentity.CommandContext
import kalix.scalasdk.valueentity.ValueEntity

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ValueEntityRouter[S, E <: ValueEntity[S]](val entity: E) {
  def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): ValueEntity.Effect[_]
}
