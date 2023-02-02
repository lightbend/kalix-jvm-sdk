package com.example.transfer;

import com.example.transfer.FraudDetectionResult.TransferRejected;
import com.example.transfer.FraudDetectionResult.TransferRequiresManualAcceptation;
import com.example.transfer.FraudDetectionResult.TransferVerified;
import com.example.transfer.TransferState.Transfer;
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

// tag::class[]
@EntityType("transfer") // <2>
@EntityKey("transferId") // <3>
@RequestMapping("/transfer/{transferId}") // <4>
public class TransferWorkflowEntity extends WorkflowEntity<TransferState> { // <1>
  // end::class[]

  // tag::start[]
  public record Withdraw(String from, int amount) { // <4>
  }

  // end::start[]

  // tag::definition[]
  public record Deposit(String to, int amount) { // <4>
  }

  // end::definition[]


  private static final Logger logger = LoggerFactory.getLogger(TransferWorkflowEntity.class);

  private final String fraudDetectionStepName = "fraud-detection";

  // tag::definition[]
  private final String withdrawStepName = "withdraw"; // <1>
  private final String depositStepName = "deposit"; // <1>

  // end::definition[]

  final private KalixClient kalixClient;

  public TransferWorkflowEntity(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  // tag::definition[]
  @Override
  public Workflow<TransferState> definition() {
    Step withdraw =
        step(withdrawStepName) // <1>
            .call((Withdraw cmd) -> kalixClient.patch("/wallet/" + cmd.from() + "/withdraw/" + cmd.amount(), String.class)) // <2>
            .andThen(this::moveToDeposit);

    Step deposit =
        step(depositStepName) // <1>
            .call((Deposit cmd) -> kalixClient.patch("/wallet/" + cmd.to() + "/deposit/" + cmd.amount(), String.class)) // <5>
            .andThen(this::finishWithSuccess);

    return workflow() // <7>
        .addStep(withdraw)
        .addStep(deposit);
  }

  private Effect.TransitionalEffect<Void> moveToDeposit(String response) {
    TransferState transferState = currentState();
    TransferState successfulWithdrawal = transferState.asSuccessfulWithdrawal();
    Deposit depositInput = new Deposit(transferState.transfer().to(), transferState.transfer().amount()); // <4>
    return effects()
        .updateState(successfulWithdrawal)
        .transition(depositInput, depositStepName); // <3>
  }

  private Effect.TransitionalEffect<Void> finishWithSuccess(String response) {
    TransferState completed = currentState().asCompleted();
    return effects()
        .updateState(completed)
        .end(); // <6>
  }
  // end::definition[]

  // tag::start[]
  @PutMapping
  public Effect<Message> startTransfer(@RequestBody Transfer transfer) {
    if (transfer.amount() <= 0) {
      return effects().error("transfer amount should be greater than zero"); // <1>
    } else if (currentState() == null) {

      TransferState initialState = new TransferState(transfer); // <2>

      Withdraw withdraw = new Withdraw(transfer.from(), transfer.amount());

      return effects()
          .updateState(initialState) // <3>
          .transition(withdraw, withdrawStepName) // <4>
          .thenReply(new Message("transfer started")); // <5>

    } else {
      return effects().reply(new Message("transfer already started")); // <6>
    }
  }
  // end::start[]

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

  // tag::get-transfer[]
  @GetMapping // <1>
  public Effect<TransferState> getTransferState() {
    if (currentState() == null) {
      return effects().error("transfer not started");
    } else {
      return effects().reply(currentState()); // <2>
    }
  }
  // end::get-transfer[]

  private CompletionStage<FraudDetectionResult> checkFrauds(Transfer transfer) {
    if (transfer.amount() < 1_000) {
      return completedFuture(new TransferVerified(transfer));
    } else if (transfer.amount() < 1_000_000) {
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
    }
    if (result instanceof TransferRejected) {
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
