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

import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;

/** This file would be managed by the codegen and provide the interface the user implements. */
public abstract class CustomerValueEntityInterface
    extends ValueEntityBase<CustomerDomain.CustomerState> {

  public abstract Effect<CustomerApi.Customer> getCustomer(
      CustomerDomain.CustomerState currentState, CustomerApi.GetCustomerRequest request);

  public abstract Effect<Empty> create(
      CustomerDomain.CustomerState currentState, CustomerApi.Customer customer);

  public abstract Effect<Empty> changeName(
      CustomerDomain.CustomerState currentState, CustomerApi.ChangeNameRequest request);
}
