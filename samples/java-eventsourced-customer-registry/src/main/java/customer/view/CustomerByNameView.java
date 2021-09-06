/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package customer.view;

// tag::process-events[]
import com.akkaserverless.javasdk.view.ViewContext;
import com.google.protobuf.Any;
import customer.domain.CustomerDomain;

// <1>
public class CustomerByNameView extends AbstractCustomerByNameView {

  public CustomerByNameView(ViewContext context) {}

  @Override
  public CustomerDomain.CustomerState emptyState() {
    return null;
  }

  @Override // <2>
  public UpdateEffect<CustomerDomain.CustomerState> processCustomerCreated(
    CustomerDomain.CustomerState state, CustomerDomain.CustomerCreated customerCreated) {
    if (state != null) {
      return updateEffects().ignore(); // already created
    } else {
      return updateEffects().updateState(customerCreated.getCustomer());
    }
  }

  @Override // <3>
  public UpdateEffect<CustomerDomain.CustomerState> processCustomerNameChanged(
    CustomerDomain.CustomerState state, CustomerDomain.CustomerNameChanged customerNameChanged) {
    return updateEffects().updateState(state.toBuilder().setName(customerNameChanged.getNewName()).build());
  }

  @Override
  public UpdateEffect<CustomerDomain.CustomerState> processCustomerAddressChanged(
    CustomerDomain.CustomerState state, CustomerDomain.CustomerAddressChanged customerAddressChanged) {
    return updateEffects().updateState(state.toBuilder().setAddress(customerAddressChanged.getNewAddress()).build());
  }

  @Override
  public UpdateEffect<CustomerDomain.CustomerState> ignoreOtherEvents(
    CustomerDomain.CustomerState state, Any any) {
    return updateEffects().ignore();
  }
}
// end::process-events[]