package customer.action;

import kalix.javasdk.action.ActionCreationContext;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerStateSubscriptionAction extends AbstractCustomerStateSubscriptionAction {

  private static final Logger logger = LoggerFactory.getLogger(CustomerStateSubscriptionAction.class);

  public CustomerStateSubscriptionAction(ActionCreationContext creationContext) {}
  // tag::upsert[]

  @Override
  public Effect<CustomerApi.Customer> onStateChange(CustomerDomain.CustomerState customerState) {

    // not populating address for public consumption
    CustomerApi.Customer customer = CustomerApi.Customer.newBuilder()
        .setCustomerId(customerState.getCustomerId())
        .setEmail(customerState.getEmail())
        .setName(customerState.getName())
        .build();


    logger.info("Publishing public customer state out: {}", customer);
    return effects().reply(customer);
  }
  // end::upsert[]

}
