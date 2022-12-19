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

import akka.Done;
import com.example.workflow.transfer.MoneyTransferApi;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;

import java.util.concurrent.CompletionStage;

import static io.grpc.Status.Code.INVALID_ARGUMENT;

public class TransferWorkflow extends Workflow<TransferWorkflow.State> {


  public static class State {

    final public String from;
    final public String to;
    final public Double amount;

    public State(String from, String to, Double amount) {
      this.from = from;
      this.to = to;
      this.amount = amount;
    }
  }


  public static class Deposit {
    final public String accountNumber;
    final public Double amount;

    public Deposit(String accountNumber, Double amount) {
      this.accountNumber = accountNumber;
      this.amount = amount;
    }
  }

  public static class Withdraw {
    final public String accountNumber;
    final public Double amount;

    public Withdraw(String accountNumber, Double amount) {
      this.accountNumber = accountNumber;
      this.amount = amount;
    }
  }

  @Override
  public State emptyState() {
    return null;
  }


  private final String withdrawStepName = "withdraw";
  private final String depositStepName = "deposit";

  @Override
  public WorkflowDef<State> definition() {

    var withdraw =
      step(withdrawStepName, "withdraw from account")
        .call((Withdraw cmd) -> deferredCall(cmd, String.class))
        .andThen((String confirmation) -> {
          var state = currentState();
          return effects()
            .updateState(state)
            .transition(new Deposit(state.to, state.amount), depositStepName);
        });


    var deposit =
      step(depositStepName, "deposit to account")
        .call((Deposit cmd) -> deferredCall(cmd, String.class))
        .andThen(__ -> effects().end());

    return workflow("transfer-workflow")
      .add(withdraw)
      .add(deposit);
  }

  public Effect<String> start(MoneyTransferApi.Transfer transfer) {

    if (transfer.getAmount() <= 0.0)
      return effects().error("Transfer amount cannot be negative.", INVALID_ARGUMENT);
    else
      return effects()
        .updateState(new State(transfer.getFrom(), transfer.getTo(), transfer.getAmount()))
        .transition(new Withdraw(transfer.getFrom(), transfer.getAmount()), withdrawStepName)
        .thenReply("started");
  }


  public Effect<String> illegalCall(MoneyTransferApi.Transfer transfer) {
    throw new IllegalArgumentException("Account is blocked");
  }

  public Effect<String> get() {
    return effects().updateState(currentState()).end().thenReply("done");
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
