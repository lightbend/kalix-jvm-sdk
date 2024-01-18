package org.example.workflow;

import com.google.protobuf.Empty;
import kalix.javasdk.impl.workflow.WorkflowRouter;
import kalix.javasdk.workflow.CommandContext;
import kalix.javasdk.workflow.Workflow;
import org.example.workflow.domain.OuterTransferState;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A workflow handler that is the glue between the Protobuf service <code>TransferWorkflowService</code>
 * and the command handler methods in the <code>TransferWorkflow</code> class.
 */
public class TransferWorkflowRouter extends WorkflowRouter<OuterTransferState.TransferState, TransferWorkflow> {

  public TransferWorkflowRouter(TransferWorkflow workflow) {
    super(workflow);
  }

  @Override
  public Workflow.Effect<?> handleCommand(
      String commandName, OuterTransferState.TransferState state, Object command, CommandContext context) {
    switch (commandName) {

      case "Start":
        return workflow().start(state, (TransferWorkflowApi.Transfer) command);

      case "GetState":
        return workflow().getState(state, (Empty) command);

      default:
        throw new WorkflowRouter.CommandHandlerNotFound(commandName);
    }
  }
}
