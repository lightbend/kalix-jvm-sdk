package org.example.workflow;

import com.google.protobuf.Empty;
import kalix.javasdk.workflow.ProtoWorkflow;
import org.example.Components;
import org.example.ComponentsImpl;
import org.example.workflow.domain.OuterTransferState;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractTransferWorkflow extends ProtoWorkflow<OuterTransferState.TransferState> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> start(OuterTransferState.TransferState currentState, TransferWorkflowApi.Transfer transfer);

  public abstract Effect<TransferWorkflowApi.Transfer> getState(OuterTransferState.TransferState currentState, Empty empty);

}
