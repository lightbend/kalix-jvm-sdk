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

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.eventsourcedentity.Snapshot;
import com.akkaserverless.javasdk.eventsourcedentity.SnapshotHandler;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;

@EventSourcedEntity(entityType = "customers")
public class CustomerEventSourcedEntity {
  private CustomerDomain.CustomerState state = CustomerDomain.CustomerState.getDefaultInstance();

  @Snapshot
  public CustomerDomain.CustomerState snapshot() {
    return state;
  }

  @SnapshotHandler
  public void handleSnapshot(CustomerDomain.CustomerState snapshot) {
    state = snapshot;
  }

  @CommandHandler
  public CustomerApi.Customer getCustomer() {
    return convertToApi(state);
  }

  @CommandHandler
  public Empty create(CustomerApi.Customer customer, CommandContext context) {
    CustomerDomain.CustomerState domainCustomer = convertToDomain(customer);
    CustomerDomain.CustomerCreated event =
        CustomerDomain.CustomerCreated.newBuilder().setCustomer(domainCustomer).build();
    context.emit(event);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty changeName(CustomerApi.ChangeNameRequest request, CommandContext context) {
    if (state.equals(CustomerDomain.CustomerState.getDefaultInstance()))
      throw context.fail("Customer must be created before name can be changed.");
    CustomerDomain.CustomerNameChanged event =
        CustomerDomain.CustomerNameChanged.newBuilder().setNewName(request.getNewName()).build();
    context.emit(event);
    return Empty.getDefaultInstance();
  }

  @EventHandler
  public void customerCreated(CustomerDomain.CustomerCreated event) {
    state = state.toBuilder().mergeFrom(event.getCustomer()).build();
  }

  @EventHandler
  public void customerNameChanged(CustomerDomain.CustomerNameChanged event) {
    state = state.toBuilder().setName(event.getNewName()).build();
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
