/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import com.example.wiring.actions.echo.Message;
import io.grpc.Status;
import kalix.javasdk.HttpResponse;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.workflow.Workflow;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("transferId")
@TypeId("transfer-workflow-without-inputs")
@RequestMapping("/transfer-without-inputs/{transferId}")
public class TransferWorkflowWithoutInputs extends Workflow<TransferState> {

  private final String withdrawStepName = "withdraw";
  private final String withdrawAsyncStepName = "withdraw-async";
  private final String depositStepName = "deposit";
  private final String depositAsyncStepName = "deposit-async";

  private ComponentClient componentClient;

  public TransferWorkflowWithoutInputs(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowDef<TransferState> definition() {
    var withdraw =
        step(withdrawStepName)
            .call(() -> {
              var transfer = currentState().transfer;
              return componentClient.forValueEntity(transfer.from).call(WalletEntity::withdraw).params(transfer.amount);
            })
            .andThen(HttpResponse.class, response -> {
              var state = currentState().withLastStep("withdrawn").accepted();
              return effects()
                  .updateState(state)
                  .transitionTo(depositStepName);
            });

    var withdrawAsync =
        step(withdrawAsyncStepName)
            .asyncCall(() -> {
              var transfer = currentState().transfer;
              return componentClient.forValueEntity(transfer.from).call(WalletEntity::withdraw).params(transfer.amount).execute();
            })
            .andThen(HttpResponse.class, response -> {
              var state = currentState().withLastStep("withdrawn").accepted();
              return effects()
                  .updateState(state)
                  .transitionTo(depositAsyncStepName);
            });


    var deposit =
        step(depositStepName)
            .call(() -> {
              var transfer = currentState().transfer;
              return componentClient.forValueEntity(transfer.to).call(WalletEntity::deposit).params(transfer.amount);
            })
            .andThen(String.class, __ -> {
              var state = currentState().withLastStep("deposited").finished();
              return effects().updateState(state).end();
            });

    var depositAsync =
        step(depositAsyncStepName)
            .asyncCall(() -> {
              var transfer = currentState().transfer;
              return componentClient.forValueEntity(transfer.to).call(WalletEntity::deposit).params(transfer.amount).execute();
            })
            .andThen(String.class, __ -> {
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
