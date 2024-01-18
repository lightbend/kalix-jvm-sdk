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

package com.example.wiring.workflowentities;

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.HttpResponse;
import kalix.javasdk.StatusCode;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.workflow.Workflow;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;

@Id("transferId")
@TypeId("transfer-workflow")
@RequestMapping("/transfer/{transferId}")
public class TransferWorkflow extends Workflow<TransferState> {

  private final String withdrawStepName = "withdraw";
  private final String depositStepName = "deposit";

  private ComponentClient componentClient;

  public TransferWorkflow(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowDef<TransferState> definition() {
    var withdraw =
        step(withdrawStepName)
            .call(Withdraw.class, cmd -> componentClient.forValueEntity(cmd.from).call(WalletEntity::withdraw).params(cmd.amount))
            .andThen(HttpResponse.class, __ -> {
              var state = currentState().withLastStep("withdrawn").accepted();

              var depositInput = new Deposit(currentState().transfer.to, currentState().transfer.amount);

              return effects()
                  .updateState(state)
                  .transitionTo(depositStepName, depositInput);
            });

    var deposit =
        step(depositStepName)
            .call(Deposit.class, cmd -> componentClient.forValueEntity(cmd.to).call(WalletEntity::deposit).params(cmd.amount)
            ).andThen(String.class, __ -> {
              var state = currentState().withLastStep("deposited").finished();
              return effects().updateState(state).end();
            });

    return workflow()
        .timeout(Duration.ofSeconds(10))
        .addStep(withdraw)
        .addStep(deposit);
  }

  @PutMapping()
  public Effect<Message> startTransfer(@RequestBody Transfer transfer) {
    if (transfer.amount <= 0.0) {
      return effects().error("Transfer amount should be greater than zero", StatusCode.ErrorCode.BAD_REQUEST);
    } else {
      if (currentState() == null) {
        return effects()
            .updateState(new TransferState(transfer, "started"))
            .transitionTo(withdrawStepName, new Withdraw(transfer.from, transfer.amount))
            .thenReply(new Message("transfer started"));
      } else {
        return effects().reply(new Message("transfer already started"));
      }
    }
  }
}
