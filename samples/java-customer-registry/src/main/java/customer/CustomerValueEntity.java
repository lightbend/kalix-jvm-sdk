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

import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;

/**
 * This is where the user will implement his business logic.
 *
 * <p>We might generate an initial version, but after that re-generation should update just the
 * interface and the users' build tooling should indicate what needs changing.
 */
public class CustomerValueEntity extends CustomerValueEntityInterface {

  private final String entityId;

  public CustomerValueEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Effect<CustomerApi.Customer> getCustomer(
      CustomerDomain.CustomerState currentState, CustomerApi.GetCustomerRequest request) {
    return effects().reply(convertToApi(currentState));
  }

  @Override
  public Effect<Empty> create(
      CustomerDomain.CustomerState currentState, CustomerApi.Customer customer) {
    CustomerDomain.CustomerState state = convertToDomain(customer);
    return effects().updateState(state).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> changeName(
      CustomerDomain.CustomerState currentState, CustomerApi.ChangeNameRequest request) {
    CustomerDomain.CustomerState updatedState =
        currentState.toBuilder().setName(request.getNewName()).build();
    return effects().updateState(updatedState).thenReply(Empty.getDefaultInstance());
  }

  public Effect<Empty> changeAddress(
      CustomerDomain.CustomerState currentState, CustomerApi.ChangeAddressRequest request) {
    CustomerDomain.CustomerState updatedState =
        currentState
            .toBuilder()
            .setAddress(convertAddressToDomain(request.getNewAddress()))
            .build();
    return effects().updateState(updatedState).thenReply(Empty.getDefaultInstance());
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

  protected CustomerDomain.CustomerState emptyState() {
    return CustomerDomain.CustomerState.getDefaultInstance();
  }
}
