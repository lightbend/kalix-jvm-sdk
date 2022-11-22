package customer.domain;

// tag::class[]
import kalix.springsdk.annotations.TypeName;
import static customer.domain.CustomerEvent.*;

public sealed interface CustomerEvent {

  @TypeName("customer-created") // <1>
  record CustomerCreated(String email, String name, Address address) implements CustomerEvent {}

  @TypeName("name-changed")
  record NameChanged(String newName) implements CustomerEvent {}

  @TypeName("address-changed")
  record AddressChanged(Address address) implements CustomerEvent {}
}
// end::class[]
