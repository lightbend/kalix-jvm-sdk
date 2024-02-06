package org.example.workflow

import com.google.protobuf.Descriptors
import com.google.protobuf.empty.EmptyProto
import kalix.scalasdk.workflow.WorkflowContext
import kalix.scalasdk.workflow.WorkflowOptions
import kalix.scalasdk.workflow.WorkflowProvider
import org.example.service.SomeActionProto
import org.example.workflow
import org.example.workflow.domain.TransferState
import org.example.workflow.domain.WorkflowDomainProto

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object TransferWorkflowProvider {
  def apply(workflowFactory: WorkflowContext => TransferWorkflow): TransferWorkflowProvider =
    new TransferWorkflowProvider(workflowFactory, WorkflowOptions.defaults)
}
class TransferWorkflowProvider private(workflowFactory: WorkflowContext => TransferWorkflow, override val options: WorkflowOptions)
  extends WorkflowProvider[TransferState, TransferWorkflow] {

  def withOptions(newOptions: WorkflowOptions): TransferWorkflowProvider =
    new TransferWorkflowProvider(workflowFactory, newOptions)

  override final val serviceDescriptor: Descriptors.ServiceDescriptor =
    WorkflowProto.javaDescriptor.findServiceByName("TransferWorkflowService")

  override final val typeId: String = "transfer-workflow"

  override final def newRouter(context: WorkflowContext): TransferWorkflowRouter =
    new TransferWorkflowRouter(workflowFactory(context))

  override final val additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    EmptyProto.javaDescriptor ::
    SomeActionProto.javaDescriptor ::
    WorkflowDomainProto.javaDescriptor ::
    WorkflowProto.javaDescriptor :: Nil
}

