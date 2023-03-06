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
import kalix.javasdk.impl.workflowentity.WorkflowEntityRouter;

public class TransferWorkflowEntityRouter extends WorkflowEntityRouter<MoneyTransferApi.State, TransferWorkflowEntity> {

  public TransferWorkflowEntityRouter(TransferWorkflowEntity transferWorkflow) {
    super(transferWorkflow);
  }

  @Override
  public WorkflowEntity.Effect handleCommand(String commandName, MoneyTransferApi.State state, Object command, CommandContext context) {
    switch (commandName) {
      case "Start":
        return workflow().start((MoneyTransferApi.Transfer) command);
      case "SignOff":
        return workflow().singOff((MoneyTransferApi.Owner) command);
      case "IllegalCall":
        return workflow().illegalCall((MoneyTransferApi.Transfer) command);
      default:
        throw new WorkflowEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
