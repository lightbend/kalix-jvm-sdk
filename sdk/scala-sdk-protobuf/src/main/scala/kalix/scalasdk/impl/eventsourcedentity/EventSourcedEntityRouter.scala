/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.eventsourcedentity

import kalix.scalasdk.eventsourcedentity.CommandContext
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity

abstract class EventSourcedEntityRouter[S, E <: EventSourcedEntity[S]](val entity: E) {
  def handleEvent(state: S, event: Any): S

  def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): EventSourcedEntity.Effect[_]
}
