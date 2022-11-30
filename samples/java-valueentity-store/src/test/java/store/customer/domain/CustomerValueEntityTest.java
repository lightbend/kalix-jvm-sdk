package store.customer.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import org.junit.Test;
import store.customer.api.CustomerApi;

import static org.junit.Assert.*;

public class CustomerValueEntityTest {

  @Test
  public void createAndGetTest() {
    CustomerValueEntityTestKit service = CustomerValueEntityTestKit.of(CustomerValueEntity::new);
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
    ValueEntityResult<Empty> createResult = service.create(customer);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());
    CustomerDomain.CustomerState currentState = service.getState();
    assertEquals("C001", currentState.getCustomerId());
    assertEquals("someone@example.com", currentState.getEmail());
    assertEquals("Some Customer", currentState.getName());
    assertTrue(currentState.hasAddress());
    assertEquals("123 Some Street", currentState.getAddress().getStreet());
    assertEquals("Some City", currentState.getAddress().getCity());
    ValueEntityResult<CustomerApi.Customer> getResult =
        service.get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C001").build());
    assertEquals(customer, getResult.getReply());
  }
}
