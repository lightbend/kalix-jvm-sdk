package org.example.domain

import kalix.javasdk.impl.valueentity.ValueEntityRouter.CommandHandlerNotFound
import kalix.scalasdk.impl.valueentity.ValueEntityRouter
import kalix.scalasdk.valueentity.CommandContext
import kalix.scalasdk.valueentity.ValueEntity
import org.example.state.CounterState
import org.example.valueentity

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A value entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command handler methods in the <code>Counter</code> class.
 */
class CounterRouter(entity: Counter) extends ValueEntityRouter[CounterState, Counter](entity) {
  def handleCommand(commandName: String, state: CounterState, command: Any, context: CommandContext): ValueEntity.Effect[_] = {
    commandName match {
      case "Increase" =>
        entity.increase(state, command.asInstanceOf[valueentity.IncreaseValue])

      case "Decrease" =>
        entity.decrease(state, command.asInstanceOf[valueentity.DecreaseValue])

      case _ =>
        throw new CommandHandlerNotFound(commandName)
    }
  }
}

