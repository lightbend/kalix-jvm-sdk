package org.example.workflow;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.workflow.WorkflowContext;
import kalix.javasdk.workflow.WorkflowOptions;
import kalix.javasdk.workflow.WorkflowProvider;
import org.example.service.SomeServiceOuterClass;
import org.example.workflow.domain.OuterTransferState;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A workflow provider that defines how to register and create the workflow for
 * the Protobuf service <code>TransferWorkflowService</code>.
 *
 * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
 */
public class TransferWorkflowProvider implements WorkflowProvider<OuterTransferState.TransferState, TransferWorkflow> {

  private final Function<WorkflowContext, TransferWorkflow> workflowFactory;
  private final WorkflowOptions options;

  /** Factory method of TransferWorkflowProvider */
  public static TransferWorkflowProvider of(Function<WorkflowContext, TransferWorkflow> workflowFactory) {
    return new TransferWorkflowProvider(workflowFactory, WorkflowOptions.defaults());
  }

  private TransferWorkflowProvider(
      Function<WorkflowContext, TransferWorkflow> workflowFactory,
      WorkflowOptions options) {
    this.workflowFactory = workflowFactory;
    this.options = options;
  }

  @Override
  public final WorkflowOptions options() {
    return options;
  }

  public final TransferWorkflowProvider withOptions(WorkflowOptions options) {
    return new TransferWorkflowProvider(workflowFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return TransferWorkflowApi.getDescriptor().findServiceByName("TransferWorkflowService");
  }

  @Override
  public final String typeId() {
    return "transfer-workflow";
  }

  @Override
  public final TransferWorkflowRouter newRouter(WorkflowContext context) {
    return new TransferWorkflowRouter(workflowFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      EmptyProto.getDescriptor(),
      OuterTransferState.getDescriptor(),
      SomeServiceOuterClass.getDescriptor(),
      TransferWorkflowApi.getDescriptor()
    };
  }
}
