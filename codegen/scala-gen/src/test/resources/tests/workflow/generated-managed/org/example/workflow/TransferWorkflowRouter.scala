package org.example.workflow

import com.google.protobuf.empty.Empty
import kalix.javasdk.impl.workflow.WorkflowRouter.CommandHandlerNotFound
import kalix.scalasdk.impl.workflow.WorkflowRouter
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.CommandContext
import org.example.workflow
import org.example.workflow.domain.TransferState

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A value entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command handler methods in the <code>Counter</code> class.
 */
class TransferWorkflowRouter(entity: TransferWorkflow) extends WorkflowRouter[TransferState, TransferWorkflow](entity) {
  def handleCommand(commandName: String, state: TransferState, command: Any, context: CommandContext): AbstractWorkflow.Effect[_] = {
    commandName match {
      case "Start" =>
        entity.start(state, command.asInstanceOf[Transfer])

      case "GetState" =>
        entity.getState(state, command.asInstanceOf[Empty])

      case _ =>
        throw new CommandHandlerNotFound(commandName)
    }
  }
}

