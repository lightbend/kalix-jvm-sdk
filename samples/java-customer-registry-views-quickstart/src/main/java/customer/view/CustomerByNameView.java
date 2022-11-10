/* This code was generated by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package customer.view;

import kalix.javasdk.view.ViewContext;
import customer.domain.CustomerDomain;

// #tag::view[]
public class CustomerByNameView extends AbstractCustomerByNameView {

  public CustomerByNameView(ViewContext context) {
  }

  @Override
  public CustomerViewModel.CustomerViewState emptyState() { // <1>
    return CustomerViewModel.CustomerViewState.getDefaultInstance();
  }

  @Override
  public UpdateEffect<CustomerViewModel.CustomerViewState> updateCustomer(
      CustomerViewModel.CustomerViewState state, CustomerDomain.CustomerState customerState) {

    CustomerViewModel.CustomerViewState newViewState = // <2>
        CustomerViewModel.CustomerViewState.newBuilder()
            .setCustomerId(customerState.getCustomerId())
            .setEmail(customerState.getEmail())
            .setName(customerState.getName())
            .build();

    return effects().updateState(newViewState); // <3>
  }
}
// #end::view[]
