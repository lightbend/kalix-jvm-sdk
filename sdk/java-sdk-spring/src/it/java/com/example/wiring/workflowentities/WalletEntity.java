/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import kalix.javasdk.HttpResponse;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("wallet")
@RequestMapping("/wallet/{id}")
public class WalletEntity extends ValueEntity<Wallet> {

  private static final Logger logger = LoggerFactory.getLogger(WalletEntity.class);

  @PostMapping("/create/{amount}")
  public Effect<String> create(@PathVariable int amount) {
    return effects().updateState(new Wallet(commandContext().entityId(), amount)).thenReply("Ok");
  }

  @PatchMapping("/withdraw/{amount}")
  public Effect<HttpResponse> withdraw(@PathVariable int amount) {
    logger.info("Withdraw from {} amount -{}", currentState().id, amount);
    if (amount > currentState().balance) {
      return effects().error("not sufficient funds");
    } else {
      return effects().updateState(currentState().withdraw(amount)).thenReply(HttpResponse.ok("ok"));
    }
  }

  @PatchMapping("/deposit/{amount}")
  public Effect<String> deposit(@PathVariable int amount) {
    logger.info("Deposit from {} amount +{}", currentState().id, amount);
    return effects().updateState(currentState().deposit(amount)).thenReply("Ok");
  }

  @GetMapping
  public Effect<Balance> get() {
    return effects().reply(new Balance(currentState().balance));
  }
}
