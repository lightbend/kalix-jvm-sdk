package customer.action;

import kalix.javasdk.action.ActionCreationContext;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerStateSubscriptionAction extends AbstractCustomerStateSubscriptionAction {

  public CustomerStateSubscriptionAction(ActionCreationContext creationContext) {}
// tag::upsert[]

  @Override
  public Effect<CustomerApi.Customer> onStateChange(CustomerDomain.CustomerState customerState) {
   CustomerApi.Address address = CustomerApi.Address.newBuilder()
            .setStreet(customerState.getAddress().getStreet())
            .setCity(customerState.getAddress().getCity())
            .build();

   CustomerApi.Customer customer = CustomerApi.Customer.newBuilder()
            .setCustomerId(customerState.getCustomerId())
            .setEmail(customerState.getEmail())
            .setName(customerState.getName())
            .setAddress(address)
            .build();


    return effects().reply(customer);
  }
// end::upsert[]

}
