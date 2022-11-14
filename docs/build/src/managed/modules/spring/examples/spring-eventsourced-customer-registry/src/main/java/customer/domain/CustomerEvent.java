package customer.domain;

// tag::class[]
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static customer.domain.CustomerEvent.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type") // <1>
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = CustomerCreated.class, name = "customer-created"),
        @JsonSubTypes.Type(value = NameChanged.class, name = "name-changed"),  // <2>
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
// end::class[]
