/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.customer;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;

@TypeId("customer")
@Id("id")
@RequestMapping("/customers/{id}")
public class CustomerEntity extends ValueEntity<CustomerEntity.Customer> {

  public record Customer(String name, Instant createdOn) {
  }

  @PutMapping
  public Effect<String> create(@RequestBody Customer customer) {
    return effects().updateState(customer).thenReply("Ok");
  }

  @GetMapping
  public Effect<CustomerEntity.Customer> get() {
    return effects().reply(currentState());
  }
}
