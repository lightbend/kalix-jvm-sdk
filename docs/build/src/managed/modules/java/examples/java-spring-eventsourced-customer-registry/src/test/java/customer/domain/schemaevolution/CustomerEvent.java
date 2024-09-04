package customer.domain.schemaevolution;

import customer.domain.Address;
import kalix.javasdk.annotations.Migration;
import kalix.javasdk.annotations.TypeName;

import java.util.Optional;

public sealed interface CustomerEvent {

  @TypeName("internal-customer-created")
    // tag::customer-created-old[]
  record CustomerCreated(String email, String name, String street, String city) implements CustomerEvent {
  }
  // end::customer-created-old[]

  @TypeName("internal-name-changed")
  // tag::name-changed-with-migration[]
  @Migration(NameChangedMigration.class) // <1>
    // tag::name-changed-new[]
  record NameChanged(String newName, Optional<String> oldName, String reason) implements CustomerEvent {
  }
  // end::name-changed-new[]
  // end::name-changed-with-migration[]

  @TypeName("internal-address-changed")
  // tag::address-changed-new[]
  @Migration(AddressChangedMigration.class)
  record AddressChanged(Address newAddress) implements CustomerEvent {
  }
  // end::address-changed-new[]
}
