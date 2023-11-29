package store.customer.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.EventSourcedResult;
import org.junit.jupiter.api.Test;
import store.customer.api.CustomerApi;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerEntityTest {

  @Test
  public void createAndGetTest() {
    CustomerEntityTestKit service = CustomerEntityTestKit.of(CustomerEntity::new);

    CustomerApi.Customer customer =
        CustomerApi.Customer.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerApi.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .build();

    CustomerDomain.CustomerState customerState =
        CustomerDomain.CustomerState.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerDomain.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .build();

    EventSourcedResult<Empty> createResult = service.create(customer);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    assertEquals(1, createResult.getAllEvents().size());
    CustomerDomain.CustomerCreated customerCreated =
        createResult.getNextEventOfType(CustomerDomain.CustomerCreated.class);
    assertEquals(customerState, customerCreated.getCustomer());

    assertEquals(customerState, service.getState());

    EventSourcedResult<CustomerApi.Customer> getResult =
        service.get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C001").build());
    assertEquals(customer, getResult.getReply());
  }

  @Test
  public void changeNameTest() {
    CustomerEntityTestKit service = CustomerEntityTestKit.of(CustomerEntity::new);

    CustomerApi.Customer customer =
        CustomerApi.Customer.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerApi.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .build();

    EventSourcedResult<Empty> createResult = service.create(customer);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    CustomerDomain.CustomerState customerState =
        CustomerDomain.CustomerState.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerDomain.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .build();

    assertEquals(customerState, service.getState());

    CustomerApi.ChangeCustomerName changeCustomerName =
        CustomerApi.ChangeCustomerName.newBuilder()
            .setCustomerId("C001")
            .setNewName("Some Name")
            .build();

    EventSourcedResult<Empty> changeNameResult = service.changeName(changeCustomerName);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    assertEquals(1, changeNameResult.getAllEvents().size());
    CustomerDomain.CustomerNameChanged customerNameChanged =
        changeNameResult.getNextEventOfType(CustomerDomain.CustomerNameChanged.class);
    assertEquals("Some Name", customerNameChanged.getNewName());

    CustomerDomain.CustomerState customerStateWithNewName =
        customerState.toBuilder().setName("Some Name").build();
    assertEquals(customerStateWithNewName, service.getState());

    CustomerApi.Customer customerWithNewName = customer.toBuilder().setName("Some Name").build();

    EventSourcedResult<CustomerApi.Customer> getResult =
        service.get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C001").build());
    assertEquals(customerWithNewName, getResult.getReply());
  }

  @Test
  public void changeAddressTest() {
    CustomerEntityTestKit service = CustomerEntityTestKit.of(CustomerEntity::new);

    CustomerApi.Customer customer =
        CustomerApi.Customer.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerApi.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .build();

    EventSourcedResult<Empty> createResult = service.create(customer);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    CustomerDomain.CustomerState customerState =
        CustomerDomain.CustomerState.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerDomain.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .build();

    assertEquals(customerState, service.getState());

    CustomerApi.ChangeCustomerAddress changeCustomerAddress =
        CustomerApi.ChangeCustomerAddress.newBuilder()
            .setCustomerId("C001")
            .setNewAddress(
                CustomerApi.Address.newBuilder()
                    .setStreet("42 Some Road")
                    .setCity("Some Other City")
                    .build())
            .build();

    EventSourcedResult<Empty> changeAddressResult = service.changeAddress(changeCustomerAddress);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    assertEquals(1, changeAddressResult.getAllEvents().size());
    CustomerDomain.CustomerAddressChanged customerAddressChanged =
        changeAddressResult.getNextEventOfType(CustomerDomain.CustomerAddressChanged.class);
    assertEquals("42 Some Road", customerAddressChanged.getNewAddress().getStreet());
    assertEquals("Some Other City", customerAddressChanged.getNewAddress().getCity());

    CustomerDomain.CustomerState customerStateWithNewAddress =
        customerState.toBuilder()
            .setAddress(
                CustomerDomain.Address.newBuilder()
                    .setStreet("42 Some Road")
                    .setCity("Some Other City")
                    .build())
            .build();
    assertEquals(customerStateWithNewAddress, service.getState());

    CustomerApi.Customer customerWithNewAddress =
        customer.toBuilder()
            .setAddress(
                CustomerApi.Address.newBuilder()
                    .setStreet("42 Some Road")
                    .setCity("Some Other City")
                    .build())
            .build();

    EventSourcedResult<CustomerApi.Customer> getResult =
        service.get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C001").build());
    assertEquals(customerWithNewAddress, getResult.getReply());
  }
}
