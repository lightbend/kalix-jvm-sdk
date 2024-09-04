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

import kalix.javasdk.valueentity.ValueEntityContext;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Customer extends AbstractCustomer {
  @SuppressWarnings("unused")
  private final String entityId;

  public Customer(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  private static final Logger logger = LoggerFactory.getLogger(Customer.class);

  @Override
  public CustomerDomain.CustomerState emptyState() {
    return CustomerDomain.CustomerState.getDefaultInstance();
  }

  // tag::create[]
  @Override
  public Effect<Empty> create(
      CustomerDomain.CustomerState currentState, CustomerApi.Customer command) {
    CustomerDomain.CustomerState state = convertToDomain(command);
    logger.info("Creating customer {}", command);
    return effects().updateState(state).thenReply(Empty.getDefaultInstance());
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
  // end::create[]

  // tag::getCustomer[]
  @Override
  public Effect<CustomerApi.Customer> getCustomer(
          CustomerDomain.CustomerState currentState,
          CustomerApi.GetCustomerRequest command) {
    if (currentState.getCustomerId().equals("")) {
      return effects().error("Customer " + command.getCustomerId() + " has not been created.");
    } else {
      return effects().reply(convertToApi(currentState));
    }
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
  // end::getCustomer[]

  @Override
  public Effect<Empty> changeName(
      CustomerDomain.CustomerState currentState, CustomerApi.ChangeNameRequest command) {
    CustomerDomain.CustomerState updatedState =
        currentState.toBuilder().setName(command.getNewName()).build();
    return effects().updateState(updatedState).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> changeAddress(
      CustomerDomain.CustomerState currentState,
      CustomerApi.ChangeAddressRequest command) {
    CustomerDomain.CustomerState updatedState =
        currentState.toBuilder().setAddress(convertAddressToDomain(command.getNewAddress())).build();
    return effects().updateState(updatedState).thenReply(Empty.getDefaultInstance());
  }



}
