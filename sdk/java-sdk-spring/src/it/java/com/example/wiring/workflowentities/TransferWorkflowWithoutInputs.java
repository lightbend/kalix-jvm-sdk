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

package com.example.wiring.workflowentities;

import com.example.wiring.actions.echo.Message;
import io.grpc.Status;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.workflowentity.WorkflowEntity;
import kalix.spring.KalixClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@EntityType("transfer-workflow-without-inputs")
@EntityKey("transferId")
@RequestMapping("/transfer-without-inputs/{transferId}")
public class TransferWorkflowWithoutInputs extends WorkflowEntity<TransferState> {

  private final String withdrawStepName = "withdraw";
  private final String withdrawAsyncStepName = "withdraw-async";
  private final String depositStepName = "deposit";
  private final String depositAsyncStepName = "deposit-async";

  private KalixClient kalixClient;

  public TransferWorkflowWithoutInputs(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @Override
  public Workflow<TransferState> definition() {
    var withdraw =
        step(withdrawStepName)
            .call(() -> {
              var transfer = currentState().transfer;
              return kalixClient.patch("/wallet/" + transfer.from + "/withdraw/" + transfer.amount, String.class);
            })
            .andThen(response -> {
              var state = currentState().withLastStep("withdrawn").accepted();
              return effects()
                  .updateState(state)
                  .transitionTo(depositStepName);
            });

    var withdrawAsync =
        step(withdrawAsyncStepName)
            .asyncCall(() -> {
              var transfer = currentState().transfer;
              return kalixClient.patch("/wallet/" + transfer.from + "/withdraw/" + transfer.amount, String.class).execute();
            })
            .andThen(response -> {
              var state = currentState().withLastStep("withdrawn").accepted();
              return effects()
                  .updateState(state)
                  .transitionTo(depositAsyncStepName);
            });


    var deposit =
        step(depositStepName)
            .call(() -> {
              var transfer = currentState().transfer;
              return kalixClient.patch("/wallet/" + transfer.to + "/deposit/" + transfer.amount, String.class);
            })
            .andThen(__ -> {
              var state = currentState().withLastStep("deposited").finished();
              return effects().updateState(state).end();
            });

    var depositAsync =
        step(depositAsyncStepName)
            .asyncCall(() -> {
              var transfer = currentState().transfer;
              return kalixClient.patch("/wallet/" + transfer.to + "/deposit/" + transfer.amount, String.class).execute();
            })
            .andThen(__ -> {
              var state = currentState().withLastStep("deposited").finished();
              return effects().updateState(state).end();
            });

    return workflow()
        .addStep(withdraw)
        .addStep(deposit)
        .addStep(withdrawAsync)
        .addStep(depositAsync);
  }

  @PutMapping()
  public Effect<Message> startTransfer(@RequestBody Transfer transfer) {
    return start(transfer, withdrawStepName);
  }

  @PutMapping("/async")
  public Effect<Message> startTransferAsync(@RequestBody Transfer transfer) {
    return start(transfer, withdrawAsyncStepName);
  }

  private Effect<Message> start(Transfer transfer, String withdrawStepName) {
    if (transfer.amount <= 0.0) {
      return effects().error("Transfer amount should be greater than zero", Status.Code.INVALID_ARGUMENT);
    } else {
      if (currentState() == null) {
        return effects()
            .updateState(new TransferState(transfer, "started"))
            .transitionTo(withdrawStepName)
            .thenReply(new Message("transfer started"));
      } else {
        return effects().reply(new Message("transfer already started"));
      }
    }
  }
}
