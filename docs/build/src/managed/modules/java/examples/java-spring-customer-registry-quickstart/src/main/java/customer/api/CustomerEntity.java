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

// tag::customer[]
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.*;
import io.grpc.Status;
import customer.domain.Address;
import customer.domain.Customer;

@TypeId("customer") // <1>
@Id("customer_id") // <2>
@RequestMapping("/customer/{customer_id}") // <3>
public class CustomerEntity extends ValueEntity<Customer> { // <4>

  @PostMapping("/create") // <5>
  public ValueEntity.Effect<String> create(@RequestBody Customer customer) { 
    if (currentState() == null)
      return effects()
        .updateState(customer) // <6>
        .thenReply("OK");  // <7>
    else
      return effects().error("Customer exists already");
  }

  @GetMapping()
  public ValueEntity.Effect<Customer> getCustomer() {
    if (currentState() == null)
      return effects().error(
          "No customer found for id '" + commandContext().entityId() + "'",
          Status.Code.NOT_FOUND
        );
    else   
      return effects().reply(currentState());
  }

  @PostMapping("/changeName/{newName}")
  public Effect<String> changeName(@PathVariable String newName) {
    Customer updatedCustomer = currentState().withName(newName);
    return effects()
            .updateState(updatedCustomer)
            .thenReply("OK");
  }

  @PostMapping("/changeAddress")
  public Effect<String> changeAddress(@RequestBody Address newAddress) {
    Customer updatedCustomer = currentState().withAddress(newAddress);
    return effects().updateState(updatedCustomer).thenReply("OK");
  }

}
// end::customer[]