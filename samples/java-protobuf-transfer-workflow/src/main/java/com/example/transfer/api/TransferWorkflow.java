package com.example.transfer.api;

import com.example.transfer.api.TransferApi.GetRequest;
import com.example.transfer.api.TransferApi.Transfer;
import com.example.transfer.domain.TransferDomain;
import com.example.transfer.domain.TransferDomain.Deposit;
import com.example.transfer.domain.TransferDomain.TransferState;
import com.example.transfer.domain.TransferDomain.Withdraw;
import com.example.wallet.api.WalletApi.DepositRequest;
import com.example.wallet.api.WalletApi.WithdrawRequest;
import com.google.protobuf.Empty;
import kalix.javasdk.workflow.WorkflowContext;

import static com.example.transfer.domain.TransferDomain.TransferStatus.COMPLETED;
import static com.example.transfer.domain.TransferDomain.TransferStatus.WITHDRAW_SUCCEED;
import static io.grpc.Status.Code.NOT_FOUND;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Workflow Service described in your com/example/transfer/wallet_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class TransferWorkflow extends AbstractTransferWorkflow {
  @SuppressWarnings("unused")
  private final String workflowId;

  public TransferWorkflow(WorkflowContext context) {
    this.workflowId = context.workflowId();
  }

  @Override
  public TransferState emptyState() {
    return null;
  }

  // tag::definition[]
  @Override
  public WorkflowDef<TransferState> definition() {
    Step withdraw =
      step("withdraw") // <1>
        .call(Withdraw.class, cmd -> {
          WithdrawRequest withdrawRequest = WithdrawRequest.newBuilder()
            .setWalletId(cmd.getFrom())
            .setAmount(cmd.getAmount())
            .build();
          return components().walletEntity().withdraw(withdrawRequest);
        }) // <2>
        .andThen(Empty.class, __ -> {
          Deposit depositInput = Deposit.newBuilder()
            .setTo(currentState().getTo())
            .setAmount(currentState().getAmount())
            .build();
          return effects()
            .updateState(currentState().toBuilder().setStatus(WITHDRAW_SUCCEED).build())
            .transitionTo("deposit", depositInput); // <3>
        });

    Step deposit =
      step("deposit") // <1>
        .call(Deposit.class, cmd -> {
          DepositRequest depositRequest = DepositRequest.newBuilder().setWalletId(cmd.getTo()).setAmount(cmd.getAmount()).build();
          return components().walletEntity().deposit(depositRequest);
        }) // <4>
        .andThen(Empty.class, __ -> {
          return effects()
            .updateState(currentState().toBuilder().setStatus(COMPLETED).build())
            .end(); // <5>
        });

    return workflow() // <6>
      .addStep(withdraw)
      .addStep(deposit);
  }
  // end::definition[]

  // tag::start[]
  @Override
  public Effect<Empty> start(TransferState currentState, Transfer transfer) {
    if (transfer.getAmount() <= 0) {
      return effects().error("transfer amount should be greater than zero"); // <1>
    } else if (currentState != null) {
      return effects().error("transfer already started"); // <2>
    } else {
      TransferState initialState = TransferState.newBuilder() // <3>
        .setFrom(transfer.getFrom())
        .setTo(transfer.getTo())
        .setAmount(transfer.getAmount())
        .setStatus(TransferDomain.TransferStatus.STARTED)
        .build();
      Withdraw withdrawInput = Withdraw.newBuilder()
        .setFrom(transfer.getFrom())
        .setAmount(transfer.getAmount())
        .build();

      return effects()
        .updateState(initialState) // <4>
        .transitionTo("withdraw", withdrawInput) // <5>
        .thenReply(Empty.getDefaultInstance()); // <6>
    }
  }
  // end::start[]

  // tag::get-transfer[]
  @Override
  public Effect<TransferState> getTransferState(TransferState currentState, GetRequest getRequest) {
    if (currentState == null) {
      return effects().error("transfer not found", NOT_FOUND);
    } else {
      return effects().reply(currentState); // <1>
    }
  }
  // end::get-transfer[]

}
