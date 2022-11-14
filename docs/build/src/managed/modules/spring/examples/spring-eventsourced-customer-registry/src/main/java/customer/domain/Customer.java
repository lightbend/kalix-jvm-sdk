package customer.domain;

// tag::class[]
public record Customer(String email, String name, Address address) {

  public Customer withName(String newName) { // <1>
    return new Customer(email, newName, address);
  }

  public Customer withAddress(Address newAddress) { // <2> 
    return new Customer(email, name, newAddress);
  }
}
// end::class[]
