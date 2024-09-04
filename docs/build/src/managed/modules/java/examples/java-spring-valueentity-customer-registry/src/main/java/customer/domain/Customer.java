package customer.domain;

public record Customer(String customerId, String email, String name, Address address) { // <1>

  public Customer withName(String newName) { // <2>
    return new Customer(customerId, email, newName, address);
  }

  public Customer withAddress(Address newAddress) { // <2>
    return new Customer(customerId, email, name, newAddress);
  }
}
