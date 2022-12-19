package store.view.joined;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import store.view.StoreViewIntegrationTest;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedCustomerOrdersViewIntegrationTest extends StoreViewIntegrationTest {

  @Autowired private WebClient webClient;

  @Test
  public void getCustomerOrders() {
    createProduct("P123", "Super Duper Thingamajig", "USD", 123, 45);
    createProduct("P987", "Awesome Whatchamacallit", "NZD", 987, 65);
    createCustomer("C001", "someone@example.com", "Some Customer", "123 Some Street", "Some City");
    createOrder("O1234", "P123", "C001", 42);
    createOrder("O5678", "P987", "C001", 7);

    {
      List<CustomerOrder> customerOrders =
          awaitCustomerOrders("C001", orders -> orders.size() >= 2);

      assertEquals(2, customerOrders.size());

      CustomerOrder customerOrder1 = customerOrders.get(0);
      assertEquals("O1234", customerOrder1.orderId());
      assertEquals("P123", customerOrder1.productId());
      assertEquals("Super Duper Thingamajig", customerOrder1.productName());
      assertEquals("USD", customerOrder1.price().currency());
      assertEquals(123, customerOrder1.price().units());
      assertEquals(45, customerOrder1.price().cents());
      assertEquals(42, customerOrder1.quantity());
      assertEquals("C001", customerOrder1.customerId());
      assertEquals("someone@example.com", customerOrder1.email());
      assertEquals("Some Customer", customerOrder1.name());
      assertEquals("123 Some Street", customerOrder1.address().street());
      assertEquals("Some City", customerOrder1.address().city());

      CustomerOrder customerOrder2 = customerOrders.get(1);
      assertEquals("O5678", customerOrder2.orderId());
      assertEquals("P987", customerOrder2.productId());
      assertEquals("Awesome Whatchamacallit", customerOrder2.productName());
      assertEquals("NZD", customerOrder2.price().currency());
      assertEquals(987, customerOrder2.price().units());
      assertEquals(65, customerOrder2.price().cents());
      assertEquals(7, customerOrder2.quantity());
      assertEquals("C001", customerOrder2.customerId());
      assertEquals("someone@example.com", customerOrder2.email());
      assertEquals("Some Customer", customerOrder2.name());
      assertEquals("123 Some Street", customerOrder2.address().street());
      assertEquals("Some City", customerOrder2.address().city());
    }

    String newCustomerName = "Some Name";
    changeCustomerName("C001", newCustomerName);

    {
      List<CustomerOrder> customerOrders =
          awaitCustomerOrders("C001", orders -> newCustomerName.equals(orders.get(0).name()));

      CustomerOrder customerOrder1 = customerOrders.get(0);
      assertEquals("O1234", customerOrder1.orderId());
      assertEquals("Some Name", customerOrder1.name());

      CustomerOrder customerOrder2 = customerOrders.get(1);
      assertEquals("O5678", customerOrder2.orderId());
      assertEquals("Some Name", customerOrder2.name());
    }

    String newProductName = "Thing Supreme";
    changeProductName("P123", newProductName);

    {
      List<CustomerOrder> customerOrders =
          awaitCustomerOrders("C001", orders -> newProductName.equals(orders.get(0).productName()));

      CustomerOrder customerOrder1 = customerOrders.get(0);
      assertEquals("O1234", customerOrder1.orderId());
      assertEquals("Thing Supreme", customerOrder1.productName());

      CustomerOrder customerOrder2 = customerOrders.get(1);
      assertEquals("O5678", customerOrder2.orderId());
      assertEquals("Awesome Whatchamacallit", customerOrder2.productName());
    }
  }

  private List<CustomerOrder> getCustomerOrders(String customerId) {
    return webClient
        .get()
        .uri("/joined-customer-orders/" + customerId)
        .retrieve()
        .bodyToFlux(CustomerOrder.class)
        .toStream()
        .collect(Collectors.toList());
  }

  private List<CustomerOrder> awaitCustomerOrders(
      String customerId, Function<List<CustomerOrder>, Boolean> condition) {
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.SECONDS)
        .until(() -> condition.apply(getCustomerOrders(customerId)));
    return getCustomerOrders(customerId);
  }
}
