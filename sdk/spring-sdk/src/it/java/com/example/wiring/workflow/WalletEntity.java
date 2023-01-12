/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wiring.workflow;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@EntityKey("id")
@EntityType("wallet")
@RequestMapping("/wallet/{id}")
public class WalletEntity extends ValueEntity<Wallet> {

  private static final Logger logger = LoggerFactory.getLogger(WalletEntity.class);

  @PostMapping("/create")
  public Effect<String> create(@PathVariable String id) {
    return effects().updateState(new Wallet(id, 100)).thenReply("Ok");
  }

  @PatchMapping("/withdraw/{amount}")
  public Effect<String> withdraw(@PathVariable int amount) {
    logger.info("Withdraw from {} amount {}", currentState().id, amount);
    return effects().updateState(currentState().withdraw(amount)).thenReply("Ok");
  }

  @PatchMapping("/deposit/{amount}")
  public Effect<String> deposit(@PathVariable int amount) {
    logger.info("Deposit from {} amount {}", currentState().id, amount);
    return effects().updateState(currentState().deposit(amount)).thenReply("Ok");
  }

  @GetMapping
  public Effect<Balance> get() {
    return effects().reply(new Balance(currentState().balance));
  }
}
