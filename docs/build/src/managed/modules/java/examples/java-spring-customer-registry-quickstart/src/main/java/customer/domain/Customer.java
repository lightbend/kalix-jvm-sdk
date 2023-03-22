package customer.domain;

public record Customer(String email, String name, Address address) { // <1>

  public Customer withName(String newName) { // <2>
    return new Customer(email, newName, address);
  }

  public Customer withAddress(Address newAddress) { // <2>
    return new Customer(email, name, newAddress);
  }
}
