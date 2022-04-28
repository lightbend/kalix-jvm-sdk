package org.example.domain

import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.CommandHandlerNotFound
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.EventHandlerNotFound
import kalix.scalasdk.eventsourcedentity.CommandContext
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.impl.eventsourcedentity.EventSourcedEntityRouter
import org.example.events.Decreased
import org.example.events.Increased
import org.example.eventsourcedentity
import org.example.state.CounterState

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * An event sourced entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command handler methods in the <code>Counter</code> class.
 */
class CounterRouter(entity: Counter) extends EventSourcedEntityRouter[CounterState, Counter](entity) {
  def handleCommand(commandName: String, state: CounterState, command: Any, context: CommandContext): EventSourcedEntity.Effect[_] = {
    commandName match {
      case "Increase" =>
        entity.increase(state, command.asInstanceOf[eventsourcedentity.IncreaseValue])

      case "Decrease" =>
        entity.decrease(state, command.asInstanceOf[eventsourcedentity.DecreaseValue])

      case _ =>
        throw new CommandHandlerNotFound(commandName)
    }
  }
  def handleEvent(state: CounterState, event: Any): CounterState = {
    event match {
      case evt: Increased =>
        entity.increased(state, evt)

      case evt: Decreased =>
        entity.decreased(state, evt)

      case _ =>
        throw new EventHandlerNotFound(event.getClass)
    }
  }
}

