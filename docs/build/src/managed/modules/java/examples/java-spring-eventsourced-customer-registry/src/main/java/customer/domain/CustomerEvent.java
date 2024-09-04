package customer.domain;

// tag::class[]

import kalix.javasdk.annotations.Migration;
import kalix.javasdk.annotations.TypeName;

public sealed interface CustomerEvent {

  @TypeName("internal-customer-created") // <1>
  // end::class[]
  @Migration(CustomerCreatedMigration.class)
    // tag::customer-created-new[]
    // tag::class[]
  record CustomerCreated(String email, String name, Address address) implements CustomerEvent {
  }
  // end::customer-created-new[]

  @TypeName("internal-name-changed")
    // tag::name-changed-old[]
  record NameChanged(String newName) implements CustomerEvent {
  }
  // end::name-changed-old[]

  @TypeName("internal-address-changed")
    // tag::address-changed-old[]
  record AddressChanged(Address address) implements CustomerEvent {
  }
  // end::address-changed-old[]
}
// end::class[]
