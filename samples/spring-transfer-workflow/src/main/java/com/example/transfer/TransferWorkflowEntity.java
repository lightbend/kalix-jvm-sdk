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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletionStage;

import static com.example.transfer.TransferState.TransferStatus.COMPLETED;
import static com.example.transfer.TransferState.TransferStatus.SUCCESSFUL_WITHDRAWAL;
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

  final private KalixClient kalixClient;

  public TransferWorkflowEntity(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  // tag::definition[]
  @Override
  public Workflow<TransferState> definition() {
    Step withdraw =
        step("withdraw") // <1>
            .call((Withdraw cmd) -> {
              String withdrawUri = "/wallet/" + cmd.from() + "/withdraw/" + cmd.amount();
              return kalixClient.patch(withdrawUri, String.class);
            }) // <2>
            .andThen(this::moveToDeposit);

    Step deposit =
        step("deposit") // <1>
            .call((Deposit cmd) -> {
              String depositUri = "/wallet/" + cmd.to() + "/deposit/" + cmd.amount();
              return kalixClient.patch(depositUri, String.class);
            }) // <5>
            .andThen(this::finishWithSuccess);

    return workflow() // <7>
        .addStep(withdraw)
        .addStep(deposit);
  }

  private Effect.TransitionalEffect<Void> moveToDeposit(String response) {
    TransferState transferState = currentState();
    TransferState successfulWithdrawal = transferState.withStatus(SUCCESSFUL_WITHDRAWAL);
    return effects()
        .updateState(successfulWithdrawal)
        .transition(new Deposit(transferState.transfer().to(), transferState.transfer().amount()), "deposit"); // <3>
  }

  private Effect.TransitionalEffect<Void> finishWithSuccess(String response) {
    TransferState completed = currentState().withStatus(COMPLETED);
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
    } else if (currentState() != null) {
      return effects().error("transfer already started"); // <2>
    } else {

      TransferState initialState = new TransferState(transfer); // <3>

      return effects()
          .updateState(initialState) // <4>
          .transition(new Withdraw(transfer.from(), transfer.amount()), "withdraw") // <5>
          .thenReply(new Message("transfer started")); // <6>
    }
  }
  // end::start[]

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
}
