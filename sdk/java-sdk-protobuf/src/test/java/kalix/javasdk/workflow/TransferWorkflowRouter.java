/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import com.example.workflow.transfer.MoneyTransferApi;
import kalix.javasdk.impl.workflow.WorkflowRouter;

public class TransferWorkflowRouter extends WorkflowRouter<MoneyTransferApi.State, TransferWorkflow> {

  public TransferWorkflowRouter(TransferWorkflow transferWorkflow) {
    super(transferWorkflow);
  }

  @Override
  public Workflow.Effect handleCommand(String commandName, MoneyTransferApi.State state, Object command, CommandContext context) {
    switch (commandName) {
      case "Start":
        return workflow().start((MoneyTransferApi.Transfer) command);
      case "SignOff":
        return workflow().singOff((MoneyTransferApi.Owner) command);
      case "IllegalCall":
        return workflow().illegalCall((MoneyTransferApi.Transfer) command);
      default:
        throw new WorkflowRouter.CommandHandlerNotFound(commandName);
    }
  }
}
