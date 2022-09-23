package customer.api;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = CustomerCreated.class, name = "customer-created"),
        @JsonSubTypes.Type(value = NameChanged.class, name = "name-changed"),
        @JsonSubTypes.Type(value = AddressChanged.class, name = "address-changed")
    })
public interface CustomerEvent {
}
