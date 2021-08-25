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

// tag::process-events[]
import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.View;
import customer.domain.CustomerDomain;

import java.util.Optional;

@View // <1>
public class CustomerView {

  @UpdateHandler // <2>
  public CustomerDomain.CustomerState processCustomerCreated(
      CustomerDomain.CustomerCreated event, Optional<CustomerDomain.CustomerState> state) {
    if (state.isPresent()) {
      return state.get(); // already created
    } else {
      return event.getCustomer();
    }
  }

  @UpdateHandler // <3>
  public CustomerDomain.CustomerState processCustomerNameChanged(
      CustomerDomain.CustomerNameChanged event, CustomerDomain.CustomerState state) {
    return state.toBuilder().setName(event.getNewName()).build();
  }
}
// end::process-events[]
