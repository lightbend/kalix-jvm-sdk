package customer.api;

import customer.api.CustomerEventsApi.Created;
import customer.api.CustomerEventsApi.NameChanged;
import customer.domain.CustomerDomain;
import customer.domain.CustomerDomain.CustomerState;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.impl.MetadataImpl;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your customer/api/customer_events.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::topic-publisher[]
public class CustomerEventsToTopicServiceAction extends AbstractCustomerEventsToTopicServiceAction {

  public CustomerEventsToTopicServiceAction(ActionCreationContext creationContext) {}

  // end::topic-publisher[]
  @Override
  public Effect<Created> transformCustomerCreated(CustomerDomain.CustomerCreated customerCreated) {
    CustomerState customer = customerCreated.getCustomer();
    String customerId = actionContext().metadata().get("ce-subject").orElseThrow();
    Metadata metadata = Metadata.EMPTY.add("ce-subject", customerId);
    Created message = Created.newBuilder()
      .setCustomerId(customer.getCustomerId())
      .setCustomerName(customer.getName())
      .setEmail(customer.getEmail())
      .build();
    return effects().reply(message, metadata);
  }

  // tag::topic-publisher[]
  @Override
  public Effect<NameChanged> transformCustomerNameChanged(CustomerDomain.CustomerNameChanged customerNameChanged) {
    String customerId = actionContext().metadata().get("ce-subject").orElseThrow(); // <1>
    Metadata metadata = Metadata.EMPTY.add("ce-subject", customerId);
    NameChanged nameChanged = NameChanged.newBuilder().setCustomerName(customerNameChanged.getNewName()).build();
    return effects().reply(nameChanged, metadata); // <2>
  }
}
// end::topic-publisher[]