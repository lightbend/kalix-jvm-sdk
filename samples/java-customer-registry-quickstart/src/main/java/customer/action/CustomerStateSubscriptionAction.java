package customer.action;

import com.akkaserverless.javasdk.action.ActionCreationContext;
import customer.domain.CustomerDomain;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerStateSubscriptionAction extends AbstractCustomerStateSubscriptionAction {

  public CustomerStateSubscriptionAction(ActionCreationContext creationContext) {}
// tag::upsert[]

  @Override
  public Effect<CustomerDomain.CustomerState> onUpsertState(CustomerDomain.CustomerState customerState) {
    return effects().reply(customerState);
  }
// end::upsert[]

}
