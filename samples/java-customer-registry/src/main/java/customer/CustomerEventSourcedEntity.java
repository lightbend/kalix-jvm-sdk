/*
 * Copyright 2019 Lightbend Inc.
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
    CustomerApi.Address address = CustomerApi.Address.getDefaultInstance();
    if (state.hasAddress()) {
      address =
          CustomerApi.Address.newBuilder()
              .setStreet(state.getAddress().getStreet())
              .setCity(state.getAddress().getCity())
              .build();
    }
    return CustomerApi.Customer.newBuilder()
        .setCustomerId(state.getCustomerId())
        .setEmail(state.getEmail())
        .setName(state.getName())
        .setAddress(address)
        .build();
  }

  @CommandHandler
  public Empty create(CustomerApi.Customer customer, CommandContext context) {
    CustomerDomain.Address address = CustomerDomain.Address.getDefaultInstance();
    if (customer.hasAddress()) {
      address =
          CustomerDomain.Address.newBuilder()
              .setStreet(customer.getAddress().getStreet())
              .setCity(customer.getAddress().getCity())
              .build();
    }
    context.emit(
        CustomerDomain.CustomerCreated.newBuilder()
            .setCustomer(
                CustomerDomain.CustomerState.newBuilder()
                    .setCustomerId(customer.getCustomerId())
                    .setEmail(customer.getEmail())
                    .setName(customer.getName())
                    .setAddress(address))
            .build());
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty changeName(CustomerApi.ChangeNameRequest request, CommandContext context) {
    if (state.equals(CustomerDomain.CustomerState.getDefaultInstance()))
      throw context.fail("Customer must be created before name can be changed.");
    context.emit(
        CustomerDomain.CustomerNameChanged.newBuilder().setNewName(request.getNewName()).build());
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
}
