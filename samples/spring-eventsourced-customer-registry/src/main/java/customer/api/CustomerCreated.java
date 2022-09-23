package customer.api;

import com.fasterxml.jackson.annotation.JsonCreator;

public class CustomerCreated implements CustomerEvent {

  final public String email;
  final public String name;
  final public Address address;

  @JsonCreator
  public CustomerCreated(String email, String name, Address address) {
    this.email = email;
    this.name = name;
    this.address = address;{}
  }
}
