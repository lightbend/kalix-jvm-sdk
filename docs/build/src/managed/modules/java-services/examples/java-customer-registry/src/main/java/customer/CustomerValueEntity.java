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

package customer;

import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;

@ValueEntity(entityType = "customers")
public class CustomerValueEntity {

  @CommandHandler
  public CustomerApi.Customer getCustomer(
      CustomerApi.GetCustomerRequest request,
      CommandContext<CustomerDomain.CustomerState> context) {
    CustomerDomain.CustomerState state =
        context.getState().orElseGet(CustomerDomain.CustomerState::getDefaultInstance);
    return convertToApi(state);
  }

  @CommandHandler
  public Empty create(
      CustomerApi.Customer customer, CommandContext<CustomerDomain.CustomerState> context) {
    CustomerDomain.CustomerState state = convertToDomain(customer);
    context.updateState(state);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty changeName(
      CustomerApi.ChangeNameRequest request, CommandContext<CustomerDomain.CustomerState> context) {
    if (context.getState().isEmpty())
      throw context.fail("Customer must be created before name can be changed.");
    CustomerDomain.CustomerState updatedState =
        context.getState().get().toBuilder().setName(request.getNewName()).build();
    context.updateState(updatedState);
    return Empty.getDefaultInstance();
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
