package customer.view;

import customer.api.Address;
import customer.api.CustomerEvent;

import java.util.Optional;

import static customer.api.CustomerEvent.*;

public record CustomerView(String email, String name, Address address) {

  public CustomerView withName(String newName) {
    return new CustomerView(email, newName, address);
  }

  public CustomerView withAddress(Address newAddress) {
    return new CustomerView(email, name, newAddress);
  }

  /**
   * Generic method to create and update CustomerView from Customer events.
   * This method is used in CustomerByName and CustomerByEmail Views
   */
  public static Optional<CustomerView> onEvent(CustomerView state, CustomerEvent event) {

    if (state == null) {
      // the only event we can receive when state is null is the CustomerCreated
      if (event instanceof CustomerCreated created) {
        return Optional.of(new CustomerView(created.email(), created.name(), created.address()));
      }
    } else {
      // when not null, we can receive the other events
      if (event instanceof NameChanged nameChanged) {
        return Optional.of(state.withName(nameChanged.newName()));
      } else if (event instanceof AddressChanged addressChanged) {
        return Optional.of(state.withAddress(addressChanged.address()));
      }
    }

    // If state is null, and we receive anything different from CustomerCreated we will end-up here.
    // That case won't happen as the delivery ordering is guaranteed, but we need to keep the compiler happy
    return Optional.empty();
  }
}
