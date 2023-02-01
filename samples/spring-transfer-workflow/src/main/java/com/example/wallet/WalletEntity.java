package com.example.wallet;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@EntityKey("id")
@EntityType("wallet")
@RequestMapping("/wallet/{id}")
public class WalletEntity extends ValueEntity<Wallet> {

  private static final Logger logger = LoggerFactory.getLogger(WalletEntity.class);

  @PostMapping("/create/{initBalance}")
  public Effect<String> create(@PathVariable String id, @PathVariable int initBalance) {
    return effects().updateState(new Wallet(id, initBalance)).thenReply("Ok");
  }

  @PatchMapping("/withdraw/{amount}")
  public Effect<String> withdraw(@PathVariable int amount) {
    var newBalance = currentState().balance() - amount;
    if (newBalance < 0) {
      return effects().error("Insufficient balance");
    } else {
      Wallet updatedWallet = currentState().withdraw(amount);
      logger.info("Withdraw walletId: [{}] amount -{} balance after {}", currentState().id(), amount, updatedWallet.balance());
      return effects().updateState(updatedWallet).thenReply("Ok");
    }
  }

  @PatchMapping("/deposit/{amount}")
  public Effect<String> deposit(@PathVariable int amount) {
    Wallet updatedWallet = currentState().deposit(amount);
    logger.info("Deposit walletId: [{}] amount +{} balance after {}", currentState().id(), amount, updatedWallet.balance());
    return effects().updateState(updatedWallet).thenReply("Ok");
  }

  @GetMapping
  public Effect<Balance> get() {
    return effects().reply(new Balance(currentState().balance()));
  }
}
