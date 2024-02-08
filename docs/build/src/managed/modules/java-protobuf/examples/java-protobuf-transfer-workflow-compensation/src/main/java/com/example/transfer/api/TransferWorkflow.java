package com.example.transfer.api;

import akka.Done;
import com.example.transfer.api.TransferApi.AcceptationTimeoutRequest;
import com.example.transfer.api.TransferApi.Transfer;
import com.example.transfer.domain.TransferDomain;
import com.example.transfer.domain.TransferDomain.Deposit;
import com.example.transfer.domain.TransferDomain.TransferState;
import com.example.transfer.domain.TransferDomain.Withdraw;
import com.example.wallet.api.WalletApi.DepositRequest;
import com.example.wallet.api.WalletApi.DepositResult;
import com.example.wallet.api.WalletApi.WithdrawRequest;
import com.example.wallet.api.WalletApi.WithdrawResult;
import com.google.protobuf.Empty;
import kalix.javasdk.workflow.Workflow;
import kalix.javasdk.workflow.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static com.example.transfer.domain.TransferDomain.TransferStatus.COMPENSATION_COMPLETED;
import static com.example.transfer.domain.TransferDomain.TransferStatus.COMPLETED;
import static com.example.transfer.domain.TransferDomain.TransferStatus.DEPOSIT_FAILED;
import static com.example.transfer.domain.TransferDomain.TransferStatus.REQUIRES_MANUAL_INTERVENTION;
import static com.example.transfer.domain.TransferDomain.TransferStatus.TRANSFER_ACCEPTATION_TIMED_OUT;
import static com.example.transfer.domain.TransferDomain.TransferStatus.WAITING_FOR_ACCEPTATION;
import static com.example.transfer.domain.TransferDomain.TransferStatus.WITHDRAW_FAILED;
import static com.example.transfer.domain.TransferDomain.TransferStatus.WITHDRAW_SUCCEED;
import static io.grpc.Status.Code.NOT_FOUND;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;

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

  private static final Logger logger = LoggerFactory.getLogger(TransferWorkflow.class);

  @Override
  public TransferState emptyState() {
    return null;
  }

  @Override
  public WorkflowDef<TransferState> definition() {
    Workflow.Step withdraw =
      step("withdraw")
        .asyncCall(Withdraw.class, cmd -> {
          logger.info("Running withdraw step: " + cmd);
          return timers().cancel("acceptationTimout-" + currentState().getTransferId()).thenCompose(__ -> {
            WithdrawRequest withdrawRequest = WithdrawRequest.newBuilder().setWalletId(cmd.getFrom()).setAmount(cmd.getAmount()).build();
            return components().walletEntity().withdraw(withdrawRequest).execute();
          });
        })
        .andThen(WithdrawResult.class, withdrawResult -> {
          if (withdrawResult.hasSucceed()) {
            Deposit depositInput = Deposit.newBuilder().setTo(currentState().getTo()).setAmount(currentState().getAmount()).build();
            return effects()
              .updateState(currentState().toBuilder().setStatus(WITHDRAW_SUCCEED).build())
              .transitionTo("deposit", depositInput);
          } else {
            logger.warn("Withdraw failed with msg: " + withdrawResult.getFailed().getMessage());
            return effects()
              .updateState(currentState().toBuilder().setStatus(WITHDRAW_FAILED).build())
              .end();
          }

        });

    // tag::compensation[]
    Step deposit =
      step("deposit")
        .call(Deposit.class, cmd -> {
          // end::compensation[]
          logger.info("Running deposit step: " + cmd);
          // tag::compensation[]
          DepositRequest depositRequest = DepositRequest.newBuilder().setWalletId(cmd.getTo()).setAmount(cmd.getAmount()).build();
          return components().walletEntity().deposit(depositRequest);
        })
        .andThen(DepositResult.class, depositResult -> { // <1>
          if (depositResult.hasSucceed()) {
            return effects()
              .updateState(currentState().toBuilder().setStatus(COMPLETED).build())
              .end(); // <2>
          } else {
            // end::compensation[]
            logger.warn("Deposit failed with msg: " + depositResult.getFailed().getMessage());
            // tag::compensation[]
            return effects()
              .updateState(currentState().toBuilder().setStatus(DEPOSIT_FAILED).build())
              .transitionTo("compensate-withdraw"); // <3>
          }
        });

    Step compensateWithdraw =
      step("compensate-withdraw") // <3>
        .call(() -> {
          // end::compensation[]
          logger.info("Running withdraw compensation");
          // tag::compensation[]
          DepositRequest refund = DepositRequest.newBuilder().setAmount(currentState().getAmount()).setWalletId(currentState().getFrom()).build();
          return components().walletEntity().deposit(refund);
        })
        .andThen(DepositResult.class, depositResult -> {
          if (depositResult.hasSucceed()) {
            return effects()
              .updateState(currentState().toBuilder().setStatus(COMPENSATION_COMPLETED).build())
              .end(); // <4>
          } else {
            throw new IllegalStateException("Expecting succeed operation but received: " + depositResult); // <5>
          }
        });
    // end::compensation[]


    // tag::step-timeout[]
    Step failoverHandler =
      step("failover-handler")
        .asyncCall(() -> {
          // end::step-timeout[]
          logger.info("Running workflow failed step");
          // tag::step-timeout[]
          return CompletableFuture.completedStage(Done.getInstance()).thenApply(__ -> Empty.getDefaultInstance());
        })
        .andThen(Empty.class, __ -> effects()
          .updateState(currentState().toBuilder().setStatus(REQUIRES_MANUAL_INTERVENTION).build())
          .end())
        .timeout(ofSeconds(1)); // <1>
    // end::step-timeout[]

    // tag::pausing[]
    Step waitForAcceptation =
      step("wait-for-acceptation")
        .asyncCall(() -> {
          AcceptationTimeoutRequest timeoutRequest = AcceptationTimeoutRequest.newBuilder().setTransferId(currentState().getTransferId()).build();
          return timers().startSingleTimer( // <1>
            "acceptationTimout-" + currentState().getTransferId(),
            ofHours(8),
            components().transferWorkflow().acceptationTimeout(timeoutRequest)
          ).thenApply(__ -> Empty.getDefaultInstance());
        })
        .andThen(Empty.class, __ ->
          effects().pause()); // <2>
    // end::pausing[]

    // tag::timeouts[]
    // tag::recover-strategy[]
    return workflow()
      // end::recover-strategy[]
      // end::timeouts[]
      // tag::timeouts[]
      .timeout(ofSeconds(5)) // <1>
      .defaultStepTimeout(ofSeconds(2)) // <2>
      // end::timeouts[]
      // tag::recover-strategy[]
      .failoverTo("failover-handler", maxRetries(0)) // <1>
      .defaultStepRecoverStrategy(maxRetries(1).failoverTo("failover-handler")) // <2>
      .addStep(withdraw)
      .addStep(deposit, maxRetries(2).failoverTo("compensate-withdraw")) // <3>
      // end::recover-strategy[]
      .addStep(compensateWithdraw)
      .addStep(waitForAcceptation)
      .addStep(failoverHandler);
  }

  @Override
  public Effect<Empty> start(TransferState currentState, Transfer transfer) {
    if (currentState != null) {
      return effects().error("transfer already started");
    } else if (transfer.getAmount() <= 0) {
      return effects().error("transfer amount should be greater than zero");
    } else if (transfer.getAmount() > 1000) {
      logger.info("Waiting for acceptation: " + transfer);
      TransferState initialState = TransferState.newBuilder()
        .setTransferId(transfer.getTransferId())
        .setFrom(transfer.getFrom())
        .setTo(transfer.getTo())
        .setAmount(transfer.getAmount())
        .setStatus(WAITING_FOR_ACCEPTATION)
        .build();
      return effects()
        .updateState(initialState)
        .transitionTo("wait-for-acceptation")
        .thenReply(Empty.getDefaultInstance());
    } else {
      logger.info("Running: " + transfer);
      TransferState initialState = TransferState.newBuilder()
        .setTransferId(transfer.getTransferId())
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
        .updateState(initialState)
        .transitionTo("withdraw", withdrawInput)
        .thenReply(Empty.getDefaultInstance());
    }
  }

  // tag::resuming[]
  @Override
  public Effect<Empty> accept(TransferState currentState, TransferApi.AcceptRequest acceptRequest) {
    if (currentState == null) {
      return effects().error("transfer not started");
    } else if (currentState.getStatus().equals(WAITING_FOR_ACCEPTATION)) { // <1>
      // end::resuming[]
      logger.info("Accepting transfer: " + currentState.getTransferId());
      // tag::resuming[]
      Withdraw withdrawInput = Withdraw.newBuilder()
        .setFrom(currentState.getFrom())
        .setAmount(currentState.getAmount())
        .build();

      return effects()
        .transitionTo("withdraw", withdrawInput)
        .thenReply(Empty.getDefaultInstance());
    } else { // <2>
      return effects().error("Cannot accept transfer with status: " + currentState.getStatus());
    }
  }
  // end::resuming[]

  @Override
  public Effect<Empty> acceptationTimeout(TransferState currentState, AcceptationTimeoutRequest acceptationTimeoutRequest) {
    if (currentState == null) {
      return effects().error("transfer not started");
    } else if (currentState.getStatus().equals(WAITING_FOR_ACCEPTATION)) {
      return effects()
        .updateState(currentState.toBuilder().setStatus(TRANSFER_ACCEPTATION_TIMED_OUT).build())
        .end()
        .thenReply(Empty.getDefaultInstance());
    } else {
      logger.info("Ignoring acceptation timeout for status: " + currentState.getStatus());
      return effects().reply(Empty.getDefaultInstance());
    }
  }

  @Override
  public Effect<TransferState> getTransferState(TransferState currentState, TransferApi.GetRequest getRequest) {
    if (currentState == null) {
      return effects().error("transfer not found", NOT_FOUND);
    } else {
      return effects().reply(currentState);
    }
  }
}
