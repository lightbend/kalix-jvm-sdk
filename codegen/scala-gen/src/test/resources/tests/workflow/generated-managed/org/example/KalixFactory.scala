package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.workflow.WorkflowContext
import org.example.service.SomeServiceAction
import org.example.service.SomeServiceActionProvider
import org.example.workflow.TransferWorkflow
import org.example.workflow.TransferWorkflowProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createTransferWorkflow: WorkflowContext => TransferWorkflow,
      createSomeServiceAction: ActionCreationContext => SomeServiceAction): Kalix = {
    val kalix = Kalix()
    kalix
      .register(SomeServiceActionProvider(createSomeServiceAction))
      .register(TransferWorkflowProvider(createTransferWorkflow))
  }
}
