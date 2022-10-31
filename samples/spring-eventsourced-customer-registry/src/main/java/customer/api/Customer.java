package customer.api;

// tag::class[]
public record Customer(String customerId, String email, String name, Address address) {

  public Customer withName(String newName) { // <1>
    return new Customer(customerId, email, newName, address);
  }

  public Customer withAddress(Address newAddress) { // <2> 
    return new Customer(customerId, email, name, newAddress);
  }
}
// end::class[]
