package customer.domain;

// tag::class[]
import kalix.javasdk.annotations.TypeName;
import static customer.domain.CustomerEvent.*;

public sealed interface CustomerEvent {

  @TypeName("internal-customer-created") // <1>
  record CustomerCreated(String email, String name, Address address) implements CustomerEvent {}

  @TypeName("internal-name-changed")
  record NameChanged(String newName) implements CustomerEvent {}

  @TypeName("internal-address-changed")
  record AddressChanged(Address address) implements CustomerEvent {}
}
// end::class[]
