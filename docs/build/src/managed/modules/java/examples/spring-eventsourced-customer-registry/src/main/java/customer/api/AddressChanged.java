package customer.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddressChanged implements CustomerEvent  {

  final public Address address;


  @JsonCreator
  public AddressChanged(@JsonProperty Address address) {
    this.address = address;
  }
}
