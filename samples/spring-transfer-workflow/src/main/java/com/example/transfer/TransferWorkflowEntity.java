package com.example.transfer;

import com.example.transfer.FraudDetectionResult.TransferRejected;
import com.example.transfer.FraudDetectionResult.TransferRequiresManualAcceptation;
import com.example.transfer.FraudDetectionResult.TransferVerified;
import kalix.javasdk.workflowentity.WorkflowEntity;
import kalix.springsdk.KalixClient;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

@EntityType("transfer-workflow")
@EntityKey("transferId")
@RequestMapping("/transfer/{transferId}")
public class TransferWorkflowEntity extends WorkflowEntity<TransferState> {


  private static final Logger logger = LoggerFactory.getLogger(TransferWorkflowEntity.class);

  private final String fraudDetectionStepName = "fraud-detection";
  private final String withdrawStepName = "withdraw";
  private final String depositStepName = "deposit";

  final private KalixClient kalixClient;

  public TransferWorkflowEntity(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @Override
  public Workflow<TransferState> definition() {
    var fraudDetection =
        step(fraudDetectionStepName)
            .asyncCall(this::checkFrauds)
            .andThen(this::processFraudDetectionResult);

    var withdraw =
        step(withdrawStepName)
            .call((Withdraw cmd) -> kalixClient.patch("/wallet/" + cmd.from() + "/withdraw/" + cmd.amount(), String.class))
            .andThen(this::moveToDeposit);

    var deposit =
        step(depositStepName)
            .call((Deposit cmd) -> kalixClient.patch("/wallet/" + cmd.to() + "/deposit/" + cmd.amount(), String.class))
            .andThen(this::finishWithSuccess);

    return workflow()
        .addStep(fraudDetection)
        .addStep(withdraw)
        .addStep(deposit);
  }

  @PutMapping
  public Effect<Message> startTransfer(@RequestBody Transfer transfer) {
    if (transfer.amount() <= 0) {
      return effects().error("transfer amount should be greater than zero");
    } else {
      if (currentState() == null) {
        logger.debug("starting transfer: {}", transfer);
        return effects()
            .updateState(new TransferState(transfer))
            .transition(transfer, fraudDetectionStepName)
            .thenReply(new Message("transfer started"));
      } else {
        return effects().reply(new Message("transfer already started"));
      }
    }
  }

  @PatchMapping("/accept")
  public Effect<Message> acceptTransfer() {
    TransferState state = currentState();
    logger.debug("accepting transfer: " + state);
    if (state == null) {
      return effects().reply(new Message("transfer not started"));
    } else if (state.requiresApproval()) {
      var withdrawInput = new Withdraw(state.transfer().from(), state.transfer().amount());
      return effects()
          .updateState(state.asAccepted())
          .transition(withdrawInput, withdrawStepName)
          .thenReply(new Message("transfer accepted"));
    } else {
      return effects().reply(new Message("transfer cannot be accepted"));
    }
  }

  @GetMapping
  public Effect<TransferState> getTransferState() {
    if (currentState() == null) {
      return effects().error("transfer not started");
    } else {
      return effects().reply(currentState());
    }
  }

  private Effect.TransitionalEffect<Void> finishWithSuccess(String response) {
    logger.debug("transfer completed: " + currentState());
    TransferState completed = currentState().asCompleted();
    return effects()
        .updateState(completed)
        .end();
  }

  private Effect.TransitionalEffect<Void> moveToDeposit(String response) {
    TransferState transferState = currentState();
    logger.debug("move to deposit: {}", transferState);

    TransferState successfulWithdrawal = transferState.asSuccessfulWithdrawal();
    var depositInput = new Deposit(transferState.transfer().to(), transferState.transfer().amount());

    return effects()
        .updateState(successfulWithdrawal)
        .transition(depositInput, depositStepName);
  }

  private CompletionStage<FraudDetectionResult> checkFrauds(Transfer transfer) {
//    return CompletableFuture.failedFuture(new IllegalStateException("asd"));
    if (transfer.amount() < 1_000) {
      return completedFuture(new TransferVerified(transfer));
    } else if (transfer.amount() >= 1_000 && transfer.amount() < 1_000_000) {
      return completedFuture(new TransferRequiresManualAcceptation(transfer));
    } else {
      return completedFuture(new TransferRejected(transfer));
    }
  }

  private Effect.TransitionalEffect<Void> processFraudDetectionResult(FraudDetectionResult result) {
    logger.debug("process fraud detection result: {}, current state {}", result, currentState());

    if (result instanceof TransferVerified transferVerified) {
      var withdrawInput = new Withdraw(transferVerified.transfer().from(), transferVerified.transfer().amount());
      TransferState accepted = currentState().asAccepted();
      logger.debug("transfer verified: {}", accepted);
      return effects()
          .updateState(accepted)
          .transition(withdrawInput, withdrawStepName);
    } else if (result instanceof TransferRequiresManualAcceptation) {
      var manualApprovalRequired = currentState().asManualApprovalRequired();
      logger.debug("transfer requires manual approval: {}", manualApprovalRequired);
      return effects()
          .updateState(manualApprovalRequired)
          .pause();
    } if (result instanceof TransferRejected) {
      TransferState rejected = currentState().asRejected();
      logger.debug("transfer rejected: " + rejected);
      return effects()
          .updateState(rejected)
          .end();
    } else {
      throw new IllegalStateException("not supported response" + result);
    }
  }
}
