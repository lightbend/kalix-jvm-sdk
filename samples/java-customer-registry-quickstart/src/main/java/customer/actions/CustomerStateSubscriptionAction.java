package customer.actions;

import com.akkaserverless.javasdk.action.ActionCreationContext;
import customer.domain.CustomerDomain;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerStateSubscriptionAction extends AbstractCustomerStateSubscriptionAction {

  public CustomerStateSubscriptionAction(ActionCreationContext creationContext) {}

  @Override
  public Effect<CustomerDomain.CustomerState> onUpdateState(CustomerDomain.CustomerState customerState) {
    return effects().reply(customerState);
  }
}
