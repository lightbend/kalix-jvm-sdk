/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.workflow

import com.google.protobuf.Descriptors
import kalix.scalasdk.impl.workflow.WorkflowRouter

/**
 * Register a workflow component in [[kalix.scalasdk.Kalix]] using a <code>WorkflowProvider</code>. The concrete
 * <code>WorkflowProvider</code> is generated for the specific workflows defined in Protobuf, for example
 * <code>TransferWorkflowProvider</code>.
 */
trait WorkflowProvider[S >: Null, E <: AbstractWorkflow[S]] {
  def options: WorkflowOptions

  def serviceDescriptor: Descriptors.ServiceDescriptor

  def typeId: String

  def newRouter(context: WorkflowContext): WorkflowRouter[S, E]

  def additionalDescriptors: Seq[Descriptors.FileDescriptor]
}
