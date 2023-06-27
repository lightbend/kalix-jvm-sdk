package com.example.wallet;

import com.example.wallet.WalletEntity.DepositResult.DepositFailed;
import com.example.wallet.WalletEntity.DepositResult.DepositSucceed;
import com.example.wallet.WalletEntity.WithdrawResult.WithdrawFailed;
import com.example.wallet.WalletEntity.WithdrawResult.WithdrawSucceed;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

// tag::wallet[]
@Id("id")
@TypeId("wallet")
@RequestMapping("/wallet/{id}")
public class WalletEntity extends ValueEntity<WalletEntity.Wallet> {

  public record Wallet(String id, int balance) {
    public Wallet withdraw(int amount) {
      return new Wallet(id, balance - amount);
    }

    public Wallet deposit(int amount) {
      return new Wallet(id, balance + amount);
    }
  }

  public record Balance(int value) {
  }

  public sealed interface WithdrawResult {
    record WithdrawFailed(String errorMsg) implements WithdrawResult {
    }

    record WithdrawSucceed() implements WithdrawResult {
    }
  }

  public sealed interface DepositResult {
    record DepositFailed(String errorMsg) implements DepositResult {
    }

    record DepositSucceed() implements DepositResult {
    }
  }

  // end::wallet[]

  private static final Logger logger = LoggerFactory.getLogger(WalletEntity.class);

  // tag::wallet[]
  @PostMapping("/create/{initBalance}")
  public Effect<String> create(@PathVariable String id, @PathVariable int initBalance) {
    return effects().updateState(new Wallet(id, initBalance)).thenReply("Ok");
  }

  @PatchMapping("/withdraw/{amount}")
  public Effect<WithdrawResult> withdraw(@PathVariable int amount) {
    var newBalance = currentState().balance() - amount;
    if (newBalance < 0) {
      return effects().reply(new WithdrawFailed("Insufficient balance"));
    } else {
      Wallet updatedWallet = currentState().withdraw(amount);
      // end::wallet[]
      logger.info("Withdraw walletId: [{}] amount -{} balance after {}", currentState().id(), amount, updatedWallet.balance());
      // tag::wallet[]
      return effects().updateState(updatedWallet).thenReply(new WithdrawSucceed());
    }
  }

  @PatchMapping("/deposit/{amount}") // <3>
  public Effect<DepositResult> deposit(@PathVariable int amount) {
    if (currentState() == null) {
      return effects().reply(new DepositFailed("Wallet [" + commandContext().entityId() + "] not exists"));
    } else {
      Wallet updatedWallet = currentState().deposit(amount);
      // end::wallet[]
      logger.info("Deposit walletId: [{}] amount +{} balance after {}", currentState().id(), amount, updatedWallet.balance());
      // tag::wallet[]
      return effects().updateState(updatedWallet).thenReply(new DepositSucceed());
    }
  }

  @GetMapping // <4>
  public Effect<Balance> get() {
    return effects().reply(new Balance(currentState().balance()));
  }
}
// end::wallet[]
