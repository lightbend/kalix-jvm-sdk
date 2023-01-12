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

package com.example.wiring.workflow;

import kalix.javasdk.workflow.Workflow;
import kalix.springsdk.KalixClient;
import kalix.springsdk.annotations.WorkflowKey;
import kalix.springsdk.annotations.WorkflowType;
import kalix.springsdk.testmodels.Message;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@WorkflowType("transfer-workflow")
@WorkflowKey("transferId")
@RequestMapping("/transfer/{transferId}")
public class TransferWorkflow extends Workflow<TransferState> {

  private final String withdrawStepName = "withdraw";
  private final String depositStepName = "deposit";

  private KalixClient kalixClient;

  public TransferWorkflow(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @Override
  public WorkflowDef<TransferState> definition() {
    var withdraw =
        step(withdrawStepName)
            .call((Withdraw cmd) -> kalixClient.patch("/wallet/" + cmd.from + "/withdraw/" + cmd.amount, String.class))
            .andThen(response -> {
              var state = currentState().withLastStep("withdrawn");

              var depositInput = new Deposit(currentState().transfer.to, currentState().transfer.amount);

              return effects()
                  .updateState(state)
                  .transition(depositInput, depositStepName);
            });

    var deposit =
        step(depositStepName)
            .call((Deposit cmd) -> kalixClient.patch("/wallet/" + cmd.to + "/deposit/" + cmd.amount, String.class))
            .andThen(__ -> {
              var state = currentState().withLastStep("deposited");
              return effects().updateState(state).end();
            });

    return workflow()
        .addStep(withdraw)
        .addStep(deposit);
  }

  @PutMapping()
  public Effect<Message> startTransfer(@RequestBody Transfer transfer) {
    if (transfer.amount <= 0.0) {
      return effects().error("Transfer amount should be greater than zero");
    } else {
      if (currentState() == null) {
        return effects()
            .updateState(new TransferState(transfer, "started"))
            .transition(new Withdraw(transfer.from, transfer.amount), withdrawStepName)
            .thenReply(new Message("transfer started"));
      } else {
        return effects().reply(new Message("transfer already started"));
      }
    }
  }
}
