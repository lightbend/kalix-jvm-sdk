package store.customer.domain;

import kalix.javasdk.annotations.TypeName;

public sealed interface CustomerEvent {
  @TypeName("customer-created")
  record CustomerCreated(String email, String name, Address address) implements CustomerEvent {}

  @TypeName("customer-name-changed")
  record CustomerNameChanged(String newName) implements CustomerEvent {}

  @TypeName("customer-address-changed")
  record CustomerAddressChanged(Address newAddress) implements CustomerEvent {}
}
