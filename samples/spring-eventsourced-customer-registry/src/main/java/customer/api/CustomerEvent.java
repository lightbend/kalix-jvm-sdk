package customer.api;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static customer.api.CustomerEvent.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = CustomerCreated.class, name = "customer-created"),
        @JsonSubTypes.Type(value = NameChanged.class, name = "name-changed"),
        @JsonSubTypes.Type(value = AddressChanged.class, name = "address-changed")
    })
public sealed interface CustomerEvent {

  record CustomerCreated(String email, String name, Address address) implements CustomerEvent {
  }

  record NameChanged(String newName) implements CustomerEvent {
  }

  record AddressChanged(Address address) implements CustomerEvent {
  }
}
