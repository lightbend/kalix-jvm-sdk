/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.javasdk.workflowentity;

import com.example.workflowentity.transfer.MoneyTransferApi;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.impl.workflowentity.WorkflowEntityOptionsImpl;
import kalix.javasdk.impl.workflowentity.WorkflowEntityRouter;

import java.util.function.Function;

public class TransferWorkflowEntityProvider implements WorkflowEntityProvider<MoneyTransferApi.State, TransferWorkflowEntity> {


  private final Function<WorkflowEntityContext, TransferWorkflowEntity> workflowFactory;

  public static TransferWorkflowEntityProvider of(Function<WorkflowEntityContext, TransferWorkflowEntity> workflowFactory) {
    return new TransferWorkflowEntityProvider(workflowFactory);
  }

  private TransferWorkflowEntityProvider(Function<WorkflowEntityContext, TransferWorkflowEntity> workflowFactory) {
    this.workflowFactory = workflowFactory;
  }


  @Override
  public String workflowName() {
    return "transfer";
  }


  @Override
  public WorkflowEntityOptions options() {
    return WorkflowEntityOptionsImpl.defaults();
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return MoneyTransferApi.getDescriptor().findServiceByName("TransferWorkflowService");
  }

  @Override
  public WorkflowEntityRouter<MoneyTransferApi.State, TransferWorkflowEntity> newRouter(WorkflowEntityContext context) {
    return new TransferWorkflowEntityRouter(workflowFactory.apply(context));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[]{
      MoneyTransferApi.getDescriptor(),
      EmptyProto.getDescriptor()
    };
  }
}
