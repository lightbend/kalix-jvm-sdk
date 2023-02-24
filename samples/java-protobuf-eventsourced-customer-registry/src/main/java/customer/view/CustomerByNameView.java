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

package customer.view;

// tag::process-events[]
import kalix.javasdk.view.ViewContext;
import com.google.protobuf.Any;
import customer.domain.CustomerDomain;
import customer.api.CustomerApi;

public class CustomerByNameView extends AbstractCustomerByNameView { // <1>

  public CustomerByNameView(ViewContext context) {}

  @Override
  public CustomerApi.Customer emptyState() { // <2>
    return null;
  }

  @Override // <3>
  public UpdateEffect<CustomerApi.Customer> processCustomerCreated(
    CustomerApi.Customer state,
    CustomerDomain.CustomerCreated customerCreated) {
    if (state != null) {
      return effects().ignore(); // already created
    } else {
      return effects().updateState(convertToApi(customerCreated.getCustomer()));
    }
  }

  @Override // <3>
  public UpdateEffect<CustomerApi.Customer> processCustomerNameChanged(
    CustomerApi.Customer state,
    CustomerDomain.CustomerNameChanged customerNameChanged) {
    return effects().updateState(
        state.toBuilder().setName(customerNameChanged.getNewName()).build());
  }

  @Override // <3>
  public UpdateEffect<CustomerApi.Customer> processCustomerAddressChanged(
    CustomerApi.Customer state,
    CustomerDomain.CustomerAddressChanged customerAddressChanged) {
    return effects().updateState(
        state.toBuilder().setAddress(convertToApi(customerAddressChanged.getNewAddress())).build());
  }

  private CustomerApi.Customer convertToApi(CustomerDomain.CustomerState s) {
    CustomerApi.Address address = CustomerApi.Address.getDefaultInstance();
    if (s.hasAddress()) {
      address = convertToApi(s.getAddress());
    }
    return CustomerApi.Customer.newBuilder()
        .setCustomerId(s.getCustomerId())
        .setEmail(s.getEmail())
        .setName(s.getName())
        .setAddress(address)
        .build();
  }

  private CustomerApi.Address convertToApi(CustomerDomain.Address a) {
    return CustomerApi.Address.newBuilder()
        .setStreet(a.getStreet())
        .setCity(a.getCity())
        .build();
  }
}
// end::process-events[]
