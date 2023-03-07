package customer.view;

import customer.domain.Address;

public record CustomerView(String email, String name, Address address) {

  public CustomerView withName(String newName) {
    return new CustomerView(email, newName, address);
  }

  public CustomerView withAddress(Address newAddress) {
    return new CustomerView(email, name, newAddress);
  }
}
