package store.customer.domain;

public record Customer(String email, String name, Address address) {
  public Customer withName(String newName) {
    return new Customer(email, newName, address);
  }

  public Customer withAddress(Address newAddress) {
    return new Customer(email, name, newAddress);
  }
}
