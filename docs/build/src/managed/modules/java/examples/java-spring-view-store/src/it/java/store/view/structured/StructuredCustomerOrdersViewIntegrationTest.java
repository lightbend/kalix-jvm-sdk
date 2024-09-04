package store.view.structured;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import store.view.StoreViewIntegrationTest;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StructuredCustomerOrdersViewIntegrationTest extends StoreViewIntegrationTest {

  @Autowired private WebClient webClient;

  @Test
  public void getCustomerOrders() {
    createProduct("P123", "Super Duper Thingamajig", "USD", 123, 45);
    createProduct("P987", "Awesome Whatchamacallit", "NZD", 987, 65);
    createCustomer("C001", "someone@example.com", "Some Customer", "123 Some Street", "Some City");
    createOrder("O1234", "P123", "C001", 42);
    createOrder("O5678", "P987", "C001", 7);

    {
      CustomerOrders customerOrders =
          awaitCustomerOrders("C001", customer -> customer.orders().size() >= 2);

      assertEquals(2, customerOrders.orders().size());

      assertEquals("C001", customerOrders.id());
      assertEquals("Some Customer", customerOrders.shipping().name());
      assertEquals("123 Some Street", customerOrders.shipping().address1());
      assertEquals("Some City", customerOrders.shipping().address2());
      assertEquals("someone@example.com", customerOrders.shipping().contactEmail());

      ProductOrder productOrder1 = customerOrders.orders().get(0);
      assertEquals("P123", productOrder1.id());
      assertEquals("Super Duper Thingamajig", productOrder1.name());
      assertEquals(42, productOrder1.quantity());
      assertEquals("USD", productOrder1.value().currency());
      assertEquals(123, productOrder1.value().units());
      assertEquals(45, productOrder1.value().cents());
      assertEquals("O1234", productOrder1.orderId());

      ProductOrder productOrder2 = customerOrders.orders().get(1);
      assertEquals("P987", productOrder2.id());
      assertEquals("Awesome Whatchamacallit", productOrder2.name());
      assertEquals(7, productOrder2.quantity());
      assertEquals("NZD", productOrder2.value().currency());
      assertEquals(987, productOrder2.value().units());
      assertEquals(65, productOrder2.value().cents());
      assertEquals("O5678", productOrder2.orderId());
    }

    String newCustomerName = "Some Name";
    changeCustomerName("C001", newCustomerName);

    {
      CustomerOrders customerOrders =
          awaitCustomerOrders("C001", customer -> newCustomerName.equals(customer.shipping().name()));

      assertEquals("Some Name", customerOrders.shipping().name());
    }

    String newProductName = "Thing Supreme";
    changeProductName("P123", newProductName);

    {
      CustomerOrders customerOrders =
          awaitCustomerOrders(
              "C001", customer -> newProductName.equals(customer.orders().get(0).name()));

      ProductOrder productOrder1 = customerOrders.orders().get(0);
      assertEquals("O1234", productOrder1.orderId());
      assertEquals("Thing Supreme", productOrder1.name());

      ProductOrder productOrder2 = customerOrders.orders().get(1);
      assertEquals("O5678", productOrder2.orderId());
      assertEquals("Awesome Whatchamacallit", productOrder2.name());
    }
  }

  private CustomerOrders getCustomerOrders(String customerId) {
    return webClient
        .get()
        .uri("/structured-customer-orders/" + customerId)
        .retrieve()
        .bodyToMono(CustomerOrders.class)
        .block();
  }

  private CustomerOrders awaitCustomerOrders(
      String customerId, Function<CustomerOrders, Boolean> condition) {
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.SECONDS)
        .until(() -> condition.apply(getCustomerOrders(customerId)));
    return getCustomerOrders(customerId);
  }
}
