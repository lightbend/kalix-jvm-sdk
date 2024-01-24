/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.javasdk.workflow;

import com.example.workflow.transfer.MoneyTransferApi;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.impl.workflow.WorkflowOptionsImpl;
import kalix.javasdk.impl.workflow.WorkflowRouter;

import java.util.function.Function;

public class TransferWorkflowProvider implements WorkflowProvider<MoneyTransferApi.State, TransferWorkflow> {


  private final Function<WorkflowContext, TransferWorkflow> workflowFactory;

  public static TransferWorkflowProvider of(Function<WorkflowContext, TransferWorkflow> workflowFactory) {
    return new TransferWorkflowProvider(workflowFactory);
  }

  private TransferWorkflowProvider(Function<WorkflowContext, TransferWorkflow> workflowFactory) {
    this.workflowFactory = workflowFactory;
  }


  @Override
  public String typeId() {
    return "transfer";
  }


  @Override
  public WorkflowOptions options() {
    return WorkflowOptionsImpl.defaults();
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return MoneyTransferApi.getDescriptor().findServiceByName("TransferWorkflowService");
  }

  @Override
  public WorkflowRouter<MoneyTransferApi.State, TransferWorkflow> newRouter(WorkflowContext context) {
    return new TransferWorkflowRouter(workflowFactory.apply(context));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[]{
      MoneyTransferApi.getDescriptor(),
      EmptyProto.getDescriptor()
    };
  }
}
