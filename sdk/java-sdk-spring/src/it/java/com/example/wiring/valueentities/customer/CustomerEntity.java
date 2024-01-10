/*
 * Copyright 2024 Lightbend Inc.
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
