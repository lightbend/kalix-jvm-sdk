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
import com.akkaserverless.javasdk.view.View;
import com.google.protobuf.Any;
import customer.domain.CustomerDomain;
import java.util.Optional;

@View // <1>
public class CustomerByNameViewImpl extends AbstractCustomerByNameViewImpl { // <2>

  @Override // <3>
  public CustomerDomain.CustomerState processCustomerCreated(
      CustomerDomain.CustomerCreated event, Optional<CustomerDomain.CustomerState> state) {
    if (state.isPresent()) {
      return state.get(); // already created
    } else {
      return event.getCustomer();
    }
  }

  @Override // <3>
  public CustomerDomain.CustomerState processCustomerNameChanged(
      CustomerDomain.CustomerNameChanged event, Optional<CustomerDomain.CustomerState> state) {
    if (state.isPresent()) {
      return state.get().toBuilder().setName(event.getNewName()).build();
    } else {
      throw new RuntimeException("Received `CustomerNameChanged`, but no state exists.");
    }
  }

  @Override // <3>
  public CustomerDomain.CustomerState ignoreOtherEvents(
      Any event, Optional<CustomerDomain.CustomerState> state) {
    return state.orElseThrow(
        () ->
            new RuntimeException(
                "Received `" + event.getClass().getSimpleName() + "`, but no state exists."));
  }
}
// end::process-events[]
