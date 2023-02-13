package customer.api;

import customer.domain.Address;
import customer.domain.Customer;
import customer.domain.CustomerEvent;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import static customer.domain.CustomerEvent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomerEntityTest {

  private Address address = new Address("Acme Street", "Acme City");
  private Customer customer = new Customer("info@acme.com", "Acme Inc.", address);


  @Test
  public void testCustomerNameChange() {

    EventSourcedTestKit<Customer, CustomerEvent, CustomerEntity> testKit = EventSourcedTestKit.of(CustomerEntity::new);
    {
      EventSourcedResult<String> result = testKit.call(e -> e.create(customer));
      assertEquals("OK", result.getReply());
      result.getNextEventOfType(CustomerCreated.class);
    }

    {
      EventSourcedResult<String> result = testKit.call(e -> e.changeName("FooBar"));
      assertEquals("OK", result.getReply());
      assertEquals("FooBar", testKit.getState().name());
      result.getNextEventOfType(NameChanged.class);
    }

  }

  @Test
  public void testCustomerAddressChange() {

    EventSourcedTestKit<Customer, CustomerEvent, CustomerEntity> testKit = EventSourcedTestKit.of(CustomerEntity::new);
    {
      EventSourcedResult<String> result = testKit.call(e -> e.create(customer));
      assertEquals("OK", result.getReply());
      result.getNextEventOfType(CustomerCreated.class);
    }

    {
      Address newAddress = new Address("Sesame Street", "Sesame City");
      EventSourcedResult<String> result = testKit.call(e -> e.changeAddress(newAddress));
      assertEquals("OK", result.getReply());
      assertEquals("Sesame Street", testKit.getState().address().street());
      assertEquals("Sesame City", testKit.getState().address().city());
      result.getNextEventOfType(AddressChanged.class);
    }

  }
}
