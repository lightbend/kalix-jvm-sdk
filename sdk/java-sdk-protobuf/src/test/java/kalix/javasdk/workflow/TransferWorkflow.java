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
import com.google.protobuf.Empty;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.impl.GrpcDeferredCall;
import kalix.javasdk.impl.MetadataImpl;

import java.util.concurrent.CompletableFuture;

import static io.grpc.Status.Code.INVALID_ARGUMENT;

public class TransferWorkflow extends ProtoWorkflow<MoneyTransferApi.State> {


  @Override
  public MoneyTransferApi.State emptyState() {
    return MoneyTransferApi.State.getDefaultInstance();
  }


  private final String withdrawStepName = "withdraw";
  private final String depositStepName = "deposit";
  private final String remoteCallStepName = "remoteCall";

  @Override
  public WorkflowDef<MoneyTransferApi.State> definition() {

    var remoteCall =
      step(remoteCallStepName)
        // just a dummy 'remote' call to exercise the API
        .asyncCall(Empty.class, start ->
          CompletableFuture.completedFuture(Empty.getDefaultInstance()))
        .andThen(Empty.class, i -> {
          var state = currentState().toBuilder().setLog("remote-call").build();
          var withdrawInput =
            MoneyTransferApi.Withdraw
              .newBuilder()
              .setAccount(state.getFrom())
              .setAmount(state.getAmount())
              .build();

          return effects()
            .updateState(state)
            .transitionTo(withdrawStepName, withdrawInput);
        });


    var withdraw =
      step(withdrawStepName)
        .call(MoneyTransferApi.Withdraw.class, cmd -> deferredCall(cmd, Empty.class))
        .andThen(Empty.class, i -> {
          var state = currentState().toBuilder().setLog("withdrawn").build();

          var depositInput =
            MoneyTransferApi.Deposit
              .newBuilder()
              .setAccount(state.getTo())
              .setAmount(state.getAmount())
              .build();

          return effects()
            .updateState(state)
            .transitionTo(depositStepName, depositInput);
        });


    var deposit =
      step(depositStepName)
        .call(MoneyTransferApi.Deposit.class, cmd -> deferredCall(cmd, Empty.class))
        .andThen(Empty.class, __ -> {
          var state = currentState().toBuilder().setLog("deposited").build();
          return effects().updateState(state).end();
        });

    return workflow()
      .addStep(remoteCall)
      .addStep(withdraw)
      .addStep(deposit);
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
          .setLog("started")
          .build();

      return effects()
        .updateState(newState)
        .pause()
        .thenReply(Empty.getDefaultInstance());
    }
  }

  public Effect<Empty> singOff(MoneyTransferApi.Owner signOff) {

    var newState =
      currentState().toBuilder()
        .addSignOffs(signOff.getName())
        .setLog("sign-off: " + signOff.getName())
        .build();

    var effect = effects().updateState(newState);

    if (newState.getSignOffsList().size() < 2)
      return effect
        .pause()
        .thenReply(Empty.getDefaultInstance());
    else {
      return effect
        .transitionTo(remoteCallStepName, Empty.getDefaultInstance())
        .thenReply(Empty.getDefaultInstance());}
  }


  /* to test what happens when a command throws an exception */
  public Effect<String> illegalCall(MoneyTransferApi.Transfer transfer) {
    throw new IllegalArgumentException("Account is blocked");
  }

  // fake a deferred call
  private <I, O> DeferredCall<I, O> deferredCall(I input, Class<O> cls) {
    return new GrpcDeferredCall<>(
      input,
      MetadataImpl.Empty(),
      "fake.Service",
      "FakeMethod",
        (Metadata metadata) -> {
          throw new RuntimeException("Fake DeferredCall can't be executed");
        }
    );
  }
}
