package com.example.transfer;

import akka.Done;
import com.example.transfer.TransferState.Transfer;
import com.example.wallet.WalletEntity;
import com.example.wallet.WalletEntity.DepositResult;
import com.example.wallet.WalletEntity.DepositResult.DepositFailed;
import com.example.wallet.WalletEntity.DepositResult.DepositSucceed;
import com.example.wallet.WalletEntity.WithdrawResult;
import com.example.wallet.WalletEntity.WithdrawResult.WithdrawFailed;
import com.example.wallet.WalletEntity.WithdrawResult.WithdrawSucceed;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletableFuture;

import static com.example.transfer.TransferState.TransferStatus.COMPENSATION_COMPLETED;
import static com.example.transfer.TransferState.TransferStatus.COMPLETED;
import static com.example.transfer.TransferState.TransferStatus.DEPOSIT_FAILED;
import static com.example.transfer.TransferState.TransferStatus.REQUIRES_MANUAL_INTERVENTION;
import static com.example.transfer.TransferState.TransferStatus.TRANSFER_ACCEPTATION_TIMED_OUT;
import static com.example.transfer.TransferState.TransferStatus.WAITING_FOR_ACCEPTATION;
import static com.example.transfer.TransferState.TransferStatus.WITHDRAW_FAILED;
import static com.example.transfer.TransferState.TransferStatus.WITHDRAW_SUCCEED;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;

@TypeId("transfer")
@Id("transferId")
@RequestMapping("/transfer/{transferId}")
public class TransferWorkflow extends Workflow<TransferState> {

  public record Withdraw(String from, int amount) {
  }

  public record Deposit(String to, int amount) {
  }


  private static final Logger logger = LoggerFactory.getLogger(TransferWorkflow.class);

  final private ComponentClient componentClient;

  public TransferWorkflow(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowDef<TransferState> definition() {
    Step withdraw =
      step("withdraw")
        .asyncCall(Withdraw.class, cmd -> {
          logger.info("Running: " + cmd);
          // cancelling the timer in case it was scheduled
          return timers().cancel("acceptationTimout-" + currentState().transferId()).thenCompose(__ ->
            componentClient.forValueEntity(cmd.from)
              .call(WalletEntity::withdraw)
              .params(cmd.amount).execute());
        })
        .andThen(WithdrawResult.class, withdrawResult -> {
          if (withdrawResult instanceof WithdrawSucceed) {
            Deposit depositInput = new Deposit(currentState().transfer().to(), currentState().transfer().amount());
            return effects()
              .updateState(currentState().withStatus(WITHDRAW_SUCCEED))
              .transitionTo("deposit", depositInput);
          } else if (withdrawResult instanceof WithdrawFailed withdrawFailed) {
            logger.warn("Withdraw failed with msg: " + withdrawFailed.errorMsg());
            return effects()
              .updateState(currentState().withStatus(WITHDRAW_FAILED))
              .end();
          } else {
            throw new IllegalStateException("not supported withdraw result: " + withdrawResult);
          }
        });

    // tag::compensation[]
    Step deposit =
      step("deposit")
        .call(Deposit.class, cmd -> {
          // end::compensation[]
          logger.info("Running: " + cmd);
          // tag::compensation[]
          return componentClient.forValueEntity(cmd.to)
            .call(WalletEntity::deposit)
            .params(cmd.amount);
        })
        .andThen(DepositResult.class, depositResult -> { // <1>
          if (depositResult instanceof DepositSucceed) {
            return effects()
              .updateState(currentState().withStatus(COMPLETED))
              .end(); // <2>
          } else if (depositResult instanceof DepositFailed depositFailed) { // <3>
            // end::compensation[]
            logger.warn("Deposit failed with msg: " + depositFailed.errorMsg());
            // tag::compensation[]
            return effects()
              .updateState(currentState().withStatus(DEPOSIT_FAILED))
              .transitionTo("compensate-withdraw"); // <4>
          } else {
            throw new IllegalStateException("not supported deposit result: " + depositResult);
          }
        });

    Step compensateWithdraw =
      step("compensate-withdraw") // <4>
        .call(() -> {
          // end::compensation[]
          logger.info("Running withdraw compensation");
          // tag::compensation[]
          var transfer = currentState().transfer();
          return componentClient.forValueEntity(transfer.from())
            .call(WalletEntity::deposit)
            .params(transfer.amount());
        })
        .andThen(DepositResult.class, depositResult -> {
          if (depositResult instanceof DepositSucceed) {
            return effects()
              .updateState(currentState().withStatus(COMPENSATION_COMPLETED))
              .end(); // <5>
          } else {
            throw new IllegalStateException("Expecting succeed operation but received: " + depositResult); // <6>
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
          return CompletableFuture.completedStage("handling failure");
        })
        .andThen(String.class, __ -> effects()
          .updateState(currentState().withStatus(REQUIRES_MANUAL_INTERVENTION))
          .end())
        .timeout(ofSeconds(1)); // <1>
    // end::step-timeout[]

    // tag::pausing[]
    Step waitForAcceptation =
      step("wait-for-acceptation")
        .asyncCall(() -> {
          String transferId = currentState().transferId();
          return timers().startSingleTimer(
            "acceptationTimout-" + transferId,
            ofHours(8),
            componentClient.forWorkflow(transferId)
              .call(TransferWorkflow::acceptationTimeout)); // <1>
        })
        .andThen(Done.class, __ ->
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

  @PutMapping
  public Effect<Message> startTransfer(@RequestBody Transfer transfer) {
    if (currentState() != null) {
      return effects().error("transfer already started");
    } else if (transfer.amount() <= 0) {
      return effects().error("transfer amount should be greater than zero");
    } else if (transfer.amount() > 1000) {
      logger.info("Waiting for acceptation: " + transfer);
      TransferState waitingForAcceptationState = new TransferState(commandContext().workflowId(), transfer)
        .withStatus(WAITING_FOR_ACCEPTATION);
      return effects()
        .updateState(waitingForAcceptationState)
        .transitionTo("wait-for-acceptation")
        .thenReply(new Message("transfer started, waiting for acceptation"));
    } else {
      logger.info("Running: " + transfer);
      TransferState initialState = new TransferState(commandContext().workflowId(), transfer);
      Withdraw withdrawInput = new Withdraw(transfer.from(), transfer.amount());
      return effects()
        .updateState(initialState)
        .transitionTo("withdraw", withdrawInput)
        .thenReply(new Message("transfer started"));
    }
  }

  @PatchMapping("/acceptation-timeout")
  public Effect<String> acceptationTimeout() {
    if (currentState() == null) {
      return effects().error("transfer not started");
    } else if (currentState().status() == WAITING_FOR_ACCEPTATION) {
      return effects()
        .updateState(currentState().withStatus(TRANSFER_ACCEPTATION_TIMED_OUT))
        .end()
        .thenReply("timed out");
    } else {
      logger.info("Ignoring acceptation timeout for status: " + currentState().status());
      return effects().reply("Ok");
    }
  }

  // tag::resuming[]
  @PatchMapping("/accept")
  public Effect<Message> accept() {
    if (currentState() == null) {
      return effects().error("transfer not started");
    } else if (currentState().status() == WAITING_FOR_ACCEPTATION) { // <1>
      Transfer transfer = currentState().transfer();
      // end::resuming[]
      logger.info("Accepting transfer: " + transfer);
      // tag::resuming[]
      Withdraw withdrawInput = new Withdraw(transfer.from(), transfer.amount());
      return effects()
        .transitionTo("withdraw", withdrawInput)
        .thenReply(new Message("transfer accepted"));
    } else { // <2>
      return effects().error("Cannot accept transfer with status: " + currentState().status());
    }
  }
  // end::resuming[]

  @GetMapping
  public Effect<TransferState> getTransferState() {
    if (currentState() == null) {
      return effects().error("transfer not started");
    } else {
      return effects().reply(currentState());
    }
  }
}
