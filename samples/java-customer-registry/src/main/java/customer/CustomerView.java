/*
 * Copyright 2021 Lightbend Inc.
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
