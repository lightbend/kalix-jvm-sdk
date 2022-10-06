package customer.api;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import static customer.api.CustomerEvent.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
//@JsonSubTypes(
//    {
//        @JsonSubTypes.Type(value = CustomerCreated.class, name = "customer-created"),
//        @JsonSubTypes.Type(value = NameChanged.class, name = "name-changed"),
//        @JsonSubTypes.Type(value = AddressChanged.class, name = "address-changed")
//    })
public sealed interface CustomerEvent {

  @JsonTypeName("customer-created")
  record CustomerCreated(String email, String name, Address address) implements CustomerEvent {
  }

  @JsonTypeName("name-changed")
  record NameChanged(String newName) implements CustomerEvent {
  }

  @JsonTypeName("address-changed")
  record AddressChanged(Address address) implements CustomerEvent {
  }
}
