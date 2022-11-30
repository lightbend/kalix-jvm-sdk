package store.customer.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.valueentity.ValueEntityContext;
import store.customer.api.CustomerApi;

public class CustomerValueEntity extends AbstractCustomerValueEntity {

  public CustomerValueEntity(ValueEntityContext context) {}

  @Override
  public CustomerDomain.CustomerState emptyState() {
    return CustomerDomain.CustomerState.getDefaultInstance();
  }

  @Override
  public Effect<Empty> create(
      CustomerDomain.CustomerState currentState, CustomerApi.Customer customer) {
    CustomerDomain.CustomerState customerState =
        CustomerDomain.CustomerState.newBuilder()
            .setCustomerId(customer.getCustomerId())
            .setEmail(customer.getEmail())
            .setName(customer.getName())
            .setAddress(
                customer.hasAddress()
                    ? CustomerDomain.Address.newBuilder()
                        .setStreet(customer.getAddress().getStreet())
                        .setCity(customer.getAddress().getCity())
                        .build()
                    : CustomerDomain.Address.getDefaultInstance())
            .build();
    return effects().updateState(customerState).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<CustomerApi.Customer> get(
      CustomerDomain.CustomerState currentState, CustomerApi.GetCustomer getCustomer) {
    CustomerApi.Customer customer =
        CustomerApi.Customer.newBuilder()
            .setCustomerId(currentState.getCustomerId())
            .setEmail(currentState.getEmail())
            .setName(currentState.getName())
            .setAddress(
                currentState.hasAddress()
                    ? CustomerApi.Address.newBuilder()
                        .setStreet(currentState.getAddress().getStreet())
                        .setCity(currentState.getAddress().getCity())
                        .build()
                    : CustomerApi.Address.getDefaultInstance())
            .build();
    return effects().reply(customer);
  }
}
