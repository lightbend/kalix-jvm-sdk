package store.customer.api;

import store.customer.domain.Address;
import store.customer.domain.Customer;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;
import store.customer.domain.CustomerEvent;

import static store.customer.domain.CustomerEvent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomerEntityTest {

  @Test
  public void testCustomerNameChange() {

    EventSourcedTestKit<Customer, CustomerEvent, CustomerEntity> testKit =
        EventSourcedTestKit.of(CustomerEntity::new);

    {
      String name = "Some Customer";
      Address address = new Address("123 Some Street", "Some City");
      Customer customer = new Customer("someone@example.com", name, address);
      EventSourcedResult<String> result = testKit.call(entity -> entity.create(customer));
      assertEquals("OK", result.getReply());
      assertEquals(name, testKit.getState().name());
      result.getNextEventOfType(CustomerCreated.class);
    }

    {
      String newName = "Some Name";
      EventSourcedResult<String> result = testKit.call(entity -> entity.changeName(newName));
      assertEquals("OK", result.getReply());
      assertEquals(newName, testKit.getState().name());
      result.getNextEventOfType(CustomerNameChanged.class);
    }
  }

  @Test
  public void testCustomerAddressChange() {

    EventSourcedTestKit<Customer, CustomerEvent, CustomerEntity> testKit =
        EventSourcedTestKit.of(CustomerEntity::new);

    {
      Address address = new Address("123 Some Street", "Some City");
      Customer customer = new Customer("someone@example.com", "Some Customer", address);
      EventSourcedResult<String> result = testKit.call(e -> e.create(customer));
      assertEquals("OK", result.getReply());
      assertEquals(address.street(), testKit.getState().address().street());
      assertEquals(address.city(), testKit.getState().address().city());
      result.getNextEventOfType(CustomerCreated.class);
    }

    {
      Address newAddress = new Address("42 Some Road", "Some Other City");
      EventSourcedResult<String> result = testKit.call(e -> e.changeAddress(newAddress));
      assertEquals("OK", result.getReply());
      assertEquals(newAddress.street(), testKit.getState().address().street());
      assertEquals(newAddress.city(), testKit.getState().address().city());
      result.getNextEventOfType(CustomerAddressChanged.class);
    }
  }
}
