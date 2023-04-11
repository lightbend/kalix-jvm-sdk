package store.view;

import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;
import store.Main;
import store.customer.domain.Address;
import store.customer.domain.Customer;
import store.order.api.CreateOrder;
import store.product.domain.Money;
import store.product.domain.Product;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestKitConfig.class)
@SpringBootTest(classes = Main.class)
@DirtiesContext // fresh testkit and proxy for each integration test
public abstract class StoreViewIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired private WebClient webClient;

  protected final Duration timeout = Duration.of(5, ChronoUnit.SECONDS);

  protected void createProduct(String id, String name, String currency, long units, int cents) {
    Product product = new Product(name, new Money(currency, units, cents));
    ResponseEntity<String> response =
        webClient
            .post()
            .uri("/product/" + id + "/create")
            .bodyValue(product)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  protected void changeProductName(String id, String newName) {
    ResponseEntity<String> response =
        webClient
            .post()
            .uri("/product/" + id + "/changeName/" + newName)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  protected void createCustomer(String id, String email, String name, String street, String city) {
    Customer customer = new Customer(email, name, new Address(street, city));
    ResponseEntity<String> response =
        webClient
            .post()
            .uri("/customer/" + id + "/create")
            .bodyValue(customer)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  protected void changeCustomerName(String id, String newName) {
    ResponseEntity<String> response =
        webClient
            .post()
            .uri("/customer/" + id + "/changeName/" + newName)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  protected void createOrder(String id, String productId, String customerId, int quantity) {
    CreateOrder createOrder = new CreateOrder(productId, customerId, quantity);
    ResponseEntity<String> response =
        webClient
            .post()
            .uri("/order/" + id + "/create")
            .bodyValue(createOrder)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
