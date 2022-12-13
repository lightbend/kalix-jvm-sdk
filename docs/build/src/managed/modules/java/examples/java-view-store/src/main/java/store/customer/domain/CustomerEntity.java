package store.customer.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import store.customer.api.CustomerApi;

public class CustomerEntity extends AbstractCustomerEntity {

  public CustomerEntity(EventSourcedEntityContext context) {}

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
    CustomerDomain.CustomerCreated customerCreated =
        CustomerDomain.CustomerCreated.newBuilder().setCustomer(customerState).build();
    return effects().emitEvent(customerCreated).thenReply(__ -> Empty.getDefaultInstance());
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

  @Override
  public Effect<Empty> changeName(
      CustomerDomain.CustomerState currentState,
      CustomerApi.ChangeCustomerName changeCustomerName) {
    CustomerDomain.CustomerNameChanged customerNameChanged =
        CustomerDomain.CustomerNameChanged.newBuilder()
            .setNewName(changeCustomerName.getNewName())
            .build();
    return effects().emitEvent(customerNameChanged).thenReply(__ -> Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> changeAddress(
      CustomerDomain.CustomerState currentState,
      CustomerApi.ChangeCustomerAddress changeCustomerAddress) {
    CustomerDomain.CustomerAddressChanged customerAddressChanged =
        CustomerDomain.CustomerAddressChanged.newBuilder()
            .setNewAddress(
                changeCustomerAddress.hasNewAddress()
                    ? CustomerDomain.Address.newBuilder()
                        .setStreet(changeCustomerAddress.getNewAddress().getStreet())
                        .setCity(changeCustomerAddress.getNewAddress().getCity())
                        .build()
                    : CustomerDomain.Address.getDefaultInstance())
            .build();
    return effects().emitEvent(customerAddressChanged).thenReply(__ -> Empty.getDefaultInstance());
  }

  @Override
  public CustomerDomain.CustomerState customerCreated(
      CustomerDomain.CustomerState currentState, CustomerDomain.CustomerCreated customerCreated) {
    return customerCreated.getCustomer();
  }

  @Override
  public CustomerDomain.CustomerState customerNameChanged(
      CustomerDomain.CustomerState currentState,
      CustomerDomain.CustomerNameChanged customerNameChanged) {
    return currentState.toBuilder().setName(customerNameChanged.getNewName()).build();
  }

  @Override
  public CustomerDomain.CustomerState customerAddressChanged(
      CustomerDomain.CustomerState currentState,
      CustomerDomain.CustomerAddressChanged customerAddressChanged) {
    return currentState.toBuilder().setAddress(customerAddressChanged.getNewAddress()).build();
  }
}
