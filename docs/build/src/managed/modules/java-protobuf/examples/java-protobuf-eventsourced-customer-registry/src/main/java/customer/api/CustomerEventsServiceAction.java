package customer.api;

import customer.domain.CustomerDomain;
import kalix.javasdk.action.ActionCreationContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your customer/api/direct_customer_events.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerEventsServiceAction extends AbstractCustomerEventsServiceAction {

  public CustomerEventsServiceAction(ActionCreationContext creationContext) {}

  // tag::transform[]
  @Override
  public Effect<CustomerEventsApi.Created> transformCustomerCreated(CustomerDomain.CustomerCreated customerCreated) {
    CustomerDomain.CustomerState customer = customerCreated.getCustomer();
    return effects().reply(CustomerEventsApi.Created.newBuilder()
        .setCustomerId(customer.getCustomerId())
        .setCustomerName(customer.getName())
        .setEmail(customer.getEmail())
        .build());
  }

  @Override
  public Effect<CustomerEventsApi.NameChanged> transformCustomerNameChanged(CustomerDomain.CustomerNameChanged customerNameChanged) {
    // Note: customer_id is not present in the event or elsewhere here, but will be available as subject id
    // from the metadata on the consuming side
    return effects().reply(CustomerEventsApi.NameChanged.newBuilder()
        .setCustomerName(customerNameChanged.getNewName())
        .build());
  }
  // end::transform[]
}
