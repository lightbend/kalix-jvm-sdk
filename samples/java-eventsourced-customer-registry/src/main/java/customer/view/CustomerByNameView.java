/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package customer.view;

// tag::process-events[]
import kalix.javasdk.view.ViewContext;
import com.google.protobuf.Any;
import customer.domain.CustomerDomain;
import customer.api.CustomerApi;

public class CustomerByNameView extends AbstractCustomerByNameView { // <1>

  public CustomerByNameView(ViewContext context) {}

  @Override
  public CustomerApi.Customer emptyState() { // <2>
    return null;
  }

  @Override // <3>
  public UpdateEffect<CustomerApi.Customer> processCustomerCreated(
    CustomerApi.Customer state,
    CustomerDomain.CustomerCreated customerCreated) {
    if (state != null) {
      return effects().ignore(); // already created
    } else {
      return effects().updateState(convertToApi(customerCreated.getCustomer()));
    }
  }

  @Override // <3>
  public UpdateEffect<CustomerApi.Customer> processCustomerNameChanged(
    CustomerApi.Customer state,
    CustomerDomain.CustomerNameChanged customerNameChanged) {
    return effects().updateState(
        state.toBuilder().setName(customerNameChanged.getNewName()).build());
  }

  @Override // <3>
  public UpdateEffect<CustomerApi.Customer> processCustomerAddressChanged(
    CustomerApi.Customer state,
    CustomerDomain.CustomerAddressChanged customerAddressChanged) {
    return effects().updateState(
        state.toBuilder().setAddress(convertToApi(customerAddressChanged.getNewAddress())).build());
  }

  @Override
  public UpdateEffect<CustomerApi.Customer> ignoreOtherEvents(
    CustomerApi.Customer state,
    Any any) {
    return effects().ignore();
  }

  private CustomerApi.Customer convertToApi(CustomerDomain.CustomerState s) {
    CustomerApi.Address address = CustomerApi.Address.getDefaultInstance();
    if (s.hasAddress()) {
      address = convertToApi(s.getAddress());
    }
    return CustomerApi.Customer.newBuilder()
        .setCustomerId(s.getCustomerId())
        .setEmail(s.getEmail())
        .setName(s.getName())
        .setAddress(address)
        .build();
  }

  private CustomerApi.Address convertToApi(CustomerDomain.Address a) {
    return CustomerApi.Address.newBuilder()
        .setStreet(a.getStreet())
        .setCity(a.getCity())
        .build();
  }
}
// end::process-events[]
