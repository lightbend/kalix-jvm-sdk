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

package com.example;

import akka.Done;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.Entity;
import org.springframework.web.bind.annotation.*;

import static akka.Done.done;

@Entity(entityKey = "id", entityType = "customer")
@RequestMapping("/customer/{id}")
public class CustomerEntity extends ValueEntity<Customer> {

  // FIXME: (in SDK) default empty state as null with no need to override
  @Override
  public Customer emptyState() {
    return null;
  }

  @PostMapping("/create")
  public ValueEntity.Effect<String> create(@RequestBody Customer customer) {
    return effects().updateState(customer).thenReply("OK");
  }

  @GetMapping()
  public ValueEntity.Effect<Customer> getCustomer() {
    return effects().reply(currentState());
  }

  @PostMapping("/changeName/{newName}")
  public Effect<String> changeName(@PathVariable String newName) {
    Customer customer = currentState();
    customer.name = newName;
    return effects().updateState(customer).thenReply("OK");
  }

  @PostMapping("/changeAddress")
  public Effect<String> changeAddress(@RequestBody Address newAddress) {
    Customer customer = currentState();
    customer.address = newAddress;
    return effects().updateState(customer).thenReply("OK");
  }

}
