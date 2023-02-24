package customer.api;

import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.testkit.ValueEntityTestKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import customer.domain.Address;
import customer.domain.Customer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomerEntityTest {

  private Address address = new Address("Acme Street", "Acme City");
  private Customer customer = new Customer("info@acme.com", "Acme Inc.", address);

  @Test
  public void testCustomerNameChange() {

    ValueEntityTestKit<Customer, CustomerEntity> testKit = ValueEntityTestKit.of(CustomerEntity::new);
    {
      ValueEntityResult<String> result = testKit.call(e -> e.create(customer));
      assertEquals("OK", result.getReply());
    }

    {
      ValueEntityResult<String> result = testKit.call(e -> e.changeName("FooBar"));
      assertEquals("OK", result.getReply());
      assertEquals("FooBar", testKit.getState().name());
    }

  }

  @Test
  public void testCustomerAddressChange() {

    ValueEntityTestKit<Customer, CustomerEntity> testKit = ValueEntityTestKit.of(CustomerEntity::new);
    {
      ValueEntityResult<String> result = testKit.call(e -> e.create(customer));
      assertEquals("OK", result.getReply());
    }

    {
      Address newAddress = new Address("Sesame Street", "Sesame City");
      ValueEntityResult<String> result = testKit.call(e -> e.changeAddress(newAddress));
      assertEquals("OK", result.getReply());
      assertEquals("Sesame Street", testKit.getState().address().street());
      assertEquals("Sesame City", testKit.getState().address().city());
    }

  }
}
