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

package customer.domain;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;

@ValueEntity(entityType = "customers")
public class CustomerValueEntity extends AbstractCustomerValueEntity {
  @SuppressWarnings("unused")
  private final String entityId;

  public CustomerValueEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }

  @Override
  public Reply<Empty> create(
      CustomerApi.Customer command, CommandContext<CustomerDomain.CustomerState> context) {
    CustomerDomain.CustomerState state = convertToDomain(command);
    context.updateState(state);
    return Reply.noReply();
  }

  @Override
  public Reply<Empty> changeName(
      CustomerApi.ChangeNameRequest command, CommandContext<CustomerDomain.CustomerState> context) {
    if (context.getState().isEmpty())
      throw context.fail("Customer must be created before name can be changed.");
    CustomerDomain.CustomerState updatedState =
        context.getState().get().toBuilder().setName(command.getNewName()).build();
    context.updateState(updatedState);
    return Reply.noReply();
  }

  @Override
  public Reply<Empty> changeAddress(
      CustomerApi.ChangeAddressRequest command,
      CommandContext<CustomerDomain.CustomerState> context) {
    if (context.getState().isEmpty())
      throw context.fail("Customer must be created before address can be changed.");
    CustomerDomain.CustomerState state = context.getState().get();
    CustomerDomain.CustomerState updatedState =
        state.toBuilder().setAddress(convertAddressToDomain(command.getNewAddress())).build();
    context.updateState(updatedState);
    return Reply.noReply();
  }

  @Override
  public Reply<CustomerApi.Customer> getCustomer(
      CustomerApi.GetCustomerRequest command,
      CommandContext<CustomerDomain.CustomerState> context) {
    CustomerDomain.CustomerState state =
        context.getState().orElseGet(CustomerDomain.CustomerState::getDefaultInstance);
    return Reply.message(convertToApi(state));
  }

  private CustomerApi.Customer convertToApi(CustomerDomain.CustomerState state) {
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

  private CustomerDomain.CustomerState convertToDomain(CustomerApi.Customer customer) {
    CustomerDomain.Address address = CustomerDomain.Address.getDefaultInstance();
    if (customer.hasAddress()) {
      address = convertAddressToDomain(customer.getAddress());
    }
    return CustomerDomain.CustomerState.newBuilder()
        .setCustomerId(customer.getCustomerId())
        .setEmail(customer.getEmail())
        .setName(customer.getName())
        .setAddress(address)
        .build();
  }

  private CustomerDomain.Address convertAddressToDomain(CustomerApi.Address address) {
    return CustomerDomain.Address.newBuilder()
        .setStreet(address.getStreet())
        .setCity(address.getCity())
        .build();
  }
}
