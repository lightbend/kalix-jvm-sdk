package org.example.workflow;

import com.google.protobuf.Empty;
import kalix.javasdk.workflow.WorkflowContext;
import org.example.workflow.domain.OuterTransferState;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Workflow Service described in your workflow.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class TransferWorkflow extends AbstractTransferWorkflow {
  @SuppressWarnings("unused")
  private final String workflowId;

  public TransferWorkflow(WorkflowContext context) {
    this.workflowId = context.workflowId();
  }

  @Override
  public OuterTransferState.TransferState emptyState() {
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty workflow state");
  }

  @Override
  public WorkflowDef<OuterTransferState.TransferState> definition() {
    throw new UnsupportedOperationException("Not implemented yet, replace with your workflow definition");
  }

  @Override
  public Effect<Empty> start(OuterTransferState.TransferState currentState, TransferWorkflowApi.Transfer transfer) {
    return effects().error("The command handler for `Start` is not implemented, yet");
  }

  @Override
  public Effect<TransferWorkflowApi.Transfer> getState(OuterTransferState.TransferState currentState, Empty empty) {
    return effects().error("The command handler for `GetState` is not implemented, yet");
  }
}
