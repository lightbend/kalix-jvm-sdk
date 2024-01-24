package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.workflow.WorkflowContext;
import org.example.service.SomeServiceAction;
import org.example.service.SomeServiceActionProvider;
import org.example.service.SomeServiceOuterClass;
import org.example.workflow.TransferWorkflow;
import org.example.workflow.TransferWorkflowApi;
import org.example.workflow.TransferWorkflowProvider;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<WorkflowContext, TransferWorkflow> createTransferWorkflow,
      Function<ActionCreationContext, SomeServiceAction> createSomeServiceAction) {
    Kalix kalix = new Kalix();
    return kalix
      .register(SomeServiceActionProvider.of(createSomeServiceAction))
      .register(TransferWorkflowProvider.of(createTransferWorkflow));
  }
}
