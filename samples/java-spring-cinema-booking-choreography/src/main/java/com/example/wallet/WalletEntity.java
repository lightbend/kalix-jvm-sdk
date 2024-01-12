package com.example.wallet;

import com.example.common.Response;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.ForwardHeaders;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.function.Function;

import static com.example.common.Response.Failure;
import static com.example.common.Response.Success;
import static com.example.wallet.Wallet.WalletCommand.*;
import static com.example.wallet.Wallet.WalletCommandError.EXPENSE_NOT_FOUND;
import static com.example.wallet.Wallet.WalletEvent.*;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static kalix.javasdk.StatusCode.ErrorCode.NOT_FOUND;

@Id("id")
@TypeId("wallet")
@RequestMapping("/wallet/{id}")
@ForwardHeaders("skip-failure-simulation")
public class WalletEntity extends EventSourcedEntity<Wallet, Wallet.WalletEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public Wallet emptyState() {
    return Wallet.EMPTY_WALLET;
  }

  @PostMapping("/create/{initialBalance}")
  public Effect<Response> create(@PathVariable String id, @PathVariable int initialBalance) {
    CreateWallet createWallet = new CreateWallet(BigDecimal.valueOf(initialBalance));
    return currentState().handleCreate(id, createWallet).fold(
      error -> errorEffect(error, createWallet),
      event -> persistEffect(event, "wallet created", createWallet)
    );
  }

  @PatchMapping("/charge")
  public Effect<Response> charge(@RequestBody ChargeWallet chargeWallet) {
    if (chargeWallet.expenseId().equals("42") && commandContext().metadata().get("skip-failure-simulation").isEmpty()) {
      logger.info("charging failed");
      return effects().error("Unexpected error for expenseId=42", INVALID_ARGUMENT);
    } else {
      return currentState().handleCharge(chargeWallet).fold(
        error -> errorEffect(error, chargeWallet),
        event -> persistEffect(event, e -> {
          if (e instanceof WalletChargeRejected) {
            return Failure.of("wallet charge rejected");
          } else {
            return Success.of("wallet charged");
          }
        }, chargeWallet)
      );
    }
  }

  @PatchMapping("/refund/{expenseId}")
  public Effect<Response> refund(@RequestBody Refund refund) {
    return currentState().handleRefund(refund).fold(
      error -> {
        if (error == EXPENSE_NOT_FOUND) {
          return effects().reply(Success.of("ignoring"));
        } else {
          return errorEffect(error, refund);
        }
      },
      event -> persistEffect(event, "funds deposited", refund)
    );
  }

  @GetMapping
  public Effect<Wallet.WalletResponse> get() {
    if (currentState().isEmpty()) {
      return effects().error("wallet not created", NOT_FOUND);
    } else {
      return effects().reply(Wallet.WalletResponse.from(currentState()));
    }
  }

  private Effect<Response> persistEffect(Wallet.WalletEvent event, Function<Wallet.WalletEvent, Response> eventToResponse, Wallet.WalletCommand walletCommand) {
    return effects()
      .emitEvent(event)
      .thenReply(__ -> {
        logger.info("processing command {} completed", walletCommand);
        return eventToResponse.apply(event);
      });
  }

  private Effect<Response> persistEffect(Wallet.WalletEvent event, String replyMessage, Wallet.WalletCommand walletCommand) {
    return persistEffect(event, e -> Success.of(replyMessage), walletCommand);
  }

  private Effect<Response> errorEffect(Wallet.WalletCommandError error, Wallet.WalletCommand walletCommand) {
    if (error.equals(Wallet.WalletCommandError.DUPLICATED_COMMAND)) {
      logger.debug("Ignoring duplicated command {}", walletCommand);
      return effects().reply(Success.of("Ignoring duplicated command"));
    } else {
      logger.warn("processing {} failed with {}", walletCommand, error);
      return effects().reply(Failure.of(error.name()));
    }
  }

  @EventHandler
  public Wallet onEvent(WalletCreated walletCreated) {
    return currentState().apply(walletCreated);
  }

  @EventHandler
  public Wallet onEvent(WalletCharged walletCharged) {
    return currentState().apply(walletCharged);
  }

  @EventHandler
  public Wallet onEvent(WalletRefunded walletRefunded) {
    return currentState().apply(walletRefunded);
  }

  @EventHandler
  public Wallet onEvent(WalletChargeRejected walletCharged) {
    return currentState().apply(walletCharged);
  }
}
