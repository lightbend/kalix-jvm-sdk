package org.example.workflow

import com.google.protobuf.empty.Empty
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.WorkflowContext
import org.example.workflow
import org.example.workflow.domain.TransferState

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class TransferWorkflow(context: WorkflowContext) extends AbstractTransferWorkflow {
  override def emptyState: TransferState =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty workflow state")

  override def definition: AbstractWorkflow.WorkflowDef[TransferState] =
    throw new UnsupportedOperationException("Not implemented yet, replace with your workflow definition")

  override def start(currentState: TransferState, transfer: Transfer): AbstractWorkflow.Effect[Empty] =
    effects.error("The command handler for `Start` is not implemented, yet")

  override def getState(currentState: TransferState, empty: Empty): AbstractWorkflow.Effect[Transfer] =
    effects.error("The command handler for `GetState` is not implemented, yet")

}

