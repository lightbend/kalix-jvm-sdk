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

package customer.api;

import customer.domain.Address;
import customer.domain.Customer;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.*;

@Id("id")
@TypeId("customer")
@RequestMapping("/customer/{id}")
public class CustomerEntity extends ValueEntity<Customer> {


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
    Customer updatedCustomer = currentState().withName(newName);
    return effects().updateState(updatedCustomer).thenReply("OK");
  }

  @PostMapping("/changeAddress")
  public Effect<String> changeAddress(@RequestBody Address newAddress) {
    Customer updatedCustomer = currentState().withAddress(newAddress);
    return effects().updateState(updatedCustomer).thenReply("OK");
  }

}
