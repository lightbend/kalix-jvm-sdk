package com.example.transfer;

import com.example.transfer.TransferState.Transfer;
import com.example.wallet.WalletEntity.DepositResult;
import com.example.wallet.WalletEntity.DepositResult.DepositFailed;
import com.example.wallet.WalletEntity.DepositResult.DepositSucceed;
import com.example.wallet.WalletEntity.WithdrawResult;
import com.example.wallet.WalletEntity.WithdrawResult.WithdrawFailed;
import com.example.wallet.WalletEntity.WithdrawResult.WithdrawSucceed;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.workflowentity.WorkflowEntity;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletableFuture;

import static com.example.transfer.TransferState.TransferStatus.COMPENSATION_COMPLETED;
import static com.example.transfer.TransferState.TransferStatus.COMPLETED;
import static com.example.transfer.TransferState.TransferStatus.DEPOSIT_FAILED;
import static com.example.transfer.TransferState.TransferStatus.REQUIRES_MANUAL_INTERVENTION;
import static com.example.transfer.TransferState.TransferStatus.WITHDRAW_FAILED;
import static com.example.transfer.TransferState.TransferStatus.WITHDRAW_SUCCEED;
import static java.time.Duration.ofSeconds;
import static kalix.javasdk.workflowentity.WorkflowEntity.RecoverStrategy.maxRetries;

@EntityType("transfer")
@EntityKey("transferId")
@RequestMapping("/transfer/{transferId}")
public class TransferWorkflow extends WorkflowEntity<TransferState> {

  public record Withdraw(String from, int amount) {
  }

  public record Deposit(String to, int amount) {
  }


  private static final Logger logger = LoggerFactory.getLogger(TransferWorkflow.class);

  final private KalixClient kalixClient;

  public TransferWorkflow(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @Override
  public Workflow<TransferState> definition() {
    Step withdraw =
      step("withdraw")
        .call(Withdraw.class, cmd -> {
          logger.info("Running: " + cmd);
          String withdrawUri = "/wallet/" + cmd.from() + "/withdraw/" + cmd.amount();
          return kalixClient.patch(withdrawUri, WithdrawResult.class);
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
          String depositUri = "/wallet/" + cmd.to() + "/deposit/" + cmd.amount();
          return kalixClient.patch(depositUri, DepositResult.class); // <1>
        })
        .andThen(DepositResult.class, depositResult -> {
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
          String refundUri = "/wallet/" + transfer.from() + "/deposit/" + transfer.amount();
          return kalixClient.patch(refundUri, DepositResult.class);
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
      .addStep(failoverHandler);
  }

  @PutMapping
  public Effect<Message> startTransfer(@RequestBody Transfer transfer) {
    if (transfer.amount() <= 0) {
      return effects().error("transfer amount should be greater than zero");
    } else if (currentState() != null) {
      return effects().error("transfer already started");
    } else {
      logger.info("Running: " + transfer);
      TransferState initialState = new TransferState(transfer);
      Withdraw withdrawInput = new Withdraw(transfer.from(), transfer.amount());
      return effects()
        .updateState(initialState)
        .transitionTo("withdraw", withdrawInput)
        .thenReply(new Message("transfer started"));
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
}
