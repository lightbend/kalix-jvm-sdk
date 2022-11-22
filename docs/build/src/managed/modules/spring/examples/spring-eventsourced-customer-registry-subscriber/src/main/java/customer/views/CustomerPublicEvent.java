package customer.views;

import kalix.springsdk.annotations.TypeName;

public interface CustomerPublicEvent {

  @TypeName("customer-created")
  public record Created(String email, String name) implements CustomerPublicEvent {}

  @TypeName("name-changed")
  public record NameChanged(String newName) implements CustomerPublicEvent {}
}
