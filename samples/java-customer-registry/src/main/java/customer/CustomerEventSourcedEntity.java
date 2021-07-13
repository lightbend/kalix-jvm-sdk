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

// tag::class[]
package customer;

import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;

@EventSourcedEntity(entityType = "customers")
public class CustomerEventSourcedEntity extends EventSourcedEntityBase<CustomerDomain.CustomerState> {

  @Override
  public CustomerDomain.CustomerState emptyState() {
    return CustomerDomain.CustomerState.getDefaultInstance();
  }

  @CommandHandler
  public Effect<CustomerApi.Customer> getCustomer(CustomerDomain.CustomerState state, CustomerApi.GetCustomerRequest command) {
    return effects().reply(convertToApi(state));
  }

  @CommandHandler
  public Effect<Empty> create(CustomerDomain.CustomerState currentState, CustomerApi.Customer request) {
    CustomerDomain.CustomerState domainCustomer = convertToDomain(request);
    CustomerDomain.CustomerCreated event =
        CustomerDomain.CustomerCreated.newBuilder().setCustomer(domainCustomer).build();
    return effects().emitEvent(event).thenReply(__ -> Empty.getDefaultInstance());
  }

  @CommandHandler
  public Effect<Empty> changeName(CustomerDomain.CustomerState currentState, CustomerApi.ChangeNameRequest request) {
    if (currentState.equals(CustomerDomain.CustomerState.getDefaultInstance())) {
      return effects().error("Customer must be created before name can be changed.");
    } else {
      CustomerDomain.CustomerNameChanged event =
          CustomerDomain.CustomerNameChanged.newBuilder().setNewName(request.getNewName()).build();
      return effects().emitEvent(event).thenReply(__ -> Empty.getDefaultInstance());
    }
  }

  @EventHandler
  public CustomerDomain.CustomerState customerCreated(CustomerDomain.CustomerState currentState, CustomerDomain.CustomerCreated event) {
    return currentState.toBuilder().mergeFrom(event.getCustomer()).build();
  }

  @EventHandler
  public CustomerDomain.CustomerState customerNameChanged(CustomerDomain.CustomerState currentState, CustomerDomain.CustomerNameChanged event) {
    return currentState.toBuilder().setName(event.getNewName()).build();
  }

  private CustomerApi.Customer convertToApi(CustomerDomain.CustomerState s) {
    CustomerApi.Address address = CustomerApi.Address.getDefaultInstance();
    if (s.hasAddress()) {
      address =
          CustomerApi.Address.newBuilder()
              .setStreet(s.getAddress().getStreet())
              .setCity(s.getAddress().getCity())
              .build();
    }
    return CustomerApi.Customer.newBuilder()
        .setCustomerId(s.getCustomerId())
        .setEmail(s.getEmail())
        .setName(s.getName())
        .setAddress(address)
        .build();
  }

  private CustomerDomain.CustomerState convertToDomain(CustomerApi.Customer customer) {
    CustomerDomain.Address address = CustomerDomain.Address.getDefaultInstance();
    if (customer.hasAddress()) {
      address =
          CustomerDomain.Address.newBuilder()
              .setStreet(customer.getAddress().getStreet())
              .setCity(customer.getAddress().getCity())
              .build();
    }
    return CustomerDomain.CustomerState.newBuilder()
        .setCustomerId(customer.getCustomerId())
        .setEmail(customer.getEmail())
        .setName(customer.getName())
        .setAddress(address)
        .build();
  }
}
