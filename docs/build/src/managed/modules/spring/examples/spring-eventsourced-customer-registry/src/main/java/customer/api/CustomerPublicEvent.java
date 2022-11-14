package customer.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = CustomerPublicEvent.Created.class, name = "customer-created"),
        @JsonSubTypes.Type(value = CustomerPublicEvent.NameChanged.class, name = "name-changed"),
    })
public interface CustomerPublicEvent {

  record Created(String email, String name) implements CustomerPublicEvent {
  }

  record NameChanged(String newName) implements CustomerPublicEvent {
  }
}
