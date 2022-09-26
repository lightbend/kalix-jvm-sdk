package customer.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import customer.api.*;

import java.util.Optional;

public class CustomerView {
  final public String email;
  final public String name;
  final public Address address;

  @JsonCreator
  public CustomerView(@JsonProperty("email") String email, @JsonProperty("name") String name, @JsonProperty("address") Address address) {
    this.email = email;
    this.name = name;
    this.address = address;
  }


  public CustomerView withName(String newName) {
    return new CustomerView( email, newName, address);
  }

  public CustomerView withAddress(Address newAddress) {
    return new CustomerView( email, name, newAddress);
  }

  /**
   * Generic method to create and update CustomerView from Customer events.
   * This method is used in CustomerByName and CustomerByEmail Views
   */
  public static Optional<CustomerView> onEvent(CustomerView state, CustomerEvent event) {

    if (state == null) {
      // the only event we can receive when state is null is the CustomerCreated
      if (event instanceof CustomerCreated) {
        CustomerCreated created = (CustomerCreated) event;
        return Optional.of(new CustomerView(created.email, created.name, created.address));
      }
    } else {
      // when not null, we can receive the other events
      if (event instanceof NameChanged) {
        NameChanged nameChanged = (NameChanged) event;
        return Optional.of(state.withName(nameChanged.newName));
      } else if (event instanceof AddressChanged) {
        AddressChanged addressChanged = (AddressChanged) event;
        return Optional.of(state.withAddress(addressChanged.address));
      }
    }

    // If state is null, and we receive anything different from CustomerCreated we will end-up here.
    // That case won't happen as the delivery ordering is guaranteed, but we need to keep the compiler happy
    return Optional.empty();
  }


}
