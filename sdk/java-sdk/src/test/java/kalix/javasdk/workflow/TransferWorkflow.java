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

package kalix.javasdk.workflow;

import com.example.workflow.transfer.MoneyTransferApi;
import com.google.protobuf.Empty;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;

import java.util.concurrent.CompletionStage;

import static io.grpc.Status.Code.INVALID_ARGUMENT;

public class TransferWorkflow extends Workflow<MoneyTransferApi.State> {



  @Override
  public MoneyTransferApi.State emptyState() {
    return MoneyTransferApi.State.getDefaultInstance();
  }


  private final String withdrawStepName = "withdraw";
  private final String depositStepName = "deposit";

  @Override
  public WorkflowDef<MoneyTransferApi.State> definition() {

    var withdraw =
      step(withdrawStepName, "withdraw from account")
        .call((MoneyTransferApi.Withdraw cmd) -> deferredCall(cmd, String.class))
        .andThen((String confirmation) -> {
          var state = currentState();
          var depositInput =
            MoneyTransferApi.Deposit
              .newBuilder()
              .setAccount(state.getTo())
              .setAmount(state.getAmount())
              .build();
          return effects()
            .updateState(state)
            .transition(depositInput, depositStepName);
        });


    var deposit =
      step(depositStepName, "deposit to account")
        .call((MoneyTransferApi.Deposit cmd) -> deferredCall(cmd, String.class))
        .andThen(__ -> effects().end());

    return workflow("transfer-workflow")
      .add(withdraw)
      .add(deposit);
  }

  public Effect<Empty> start(MoneyTransferApi.Transfer transfer) {

    if (transfer.getAmount() <= 0.0)
      return effects().error("Transfer amount cannot be negative.", INVALID_ARGUMENT);
    else {

      var newState =
        MoneyTransferApi.State.newBuilder()
          .setTo(transfer.getTo())
          .setFrom(transfer.getFrom())
          .setAmount(transfer.getAmount())
          .build();

      var withdrawInput =
        MoneyTransferApi.Withdraw
          .newBuilder()
          .setAccount(transfer.getFrom())
          .setAmount(transfer.getAmount())
          .build();

      return effects()
        .updateState(newState)
        .transition(withdrawInput, withdrawStepName)
        .thenReply(Empty.getDefaultInstance());
    }
  }


  public Effect<String> illegalCall(MoneyTransferApi.Transfer transfer) {
    throw new IllegalArgumentException("Account is blocked");
  }

  private <I, O> DeferredCall<I, O> deferredCall(I input, Class<O> cls) {
    return new DeferredCall<I, O>() {
      @Override
      public I message() {
        return input;
      }

      @Override
      public Metadata metadata() {
        return Metadata.EMPTY;
      }

      @Override
      public CompletionStage<O> execute() {
        return null;
      }
    };
  }
}
