package customer;


import customer.domain.Address;
import customer.domain.Customer;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class CustomerIntegrationTest extends KalixIntegrationTestKitSupport {

  public record CustomersResponse(Collection<Customer> customers) { }

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void create() throws InterruptedException {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer(id, "foo@example.com", "Johanna", null);

    addCustomer(customer);
    Assertions.assertEquals("Johanna", getCustomerById(id).name());
  }

  private Customer getCustomerById(String customerId) {
    return webClient
        .get()
        .uri("/customer/" + customerId)
        .retrieve()
        .bodyToMono(Customer.class)
        .block(timeout);
  }

  @Test
  public void searchByCity() {
    Customer johanna = new Customer(UUID.randomUUID().toString(), "johanna@example.com", "Johanna", new Address("Cool Street", "Porto"));
    Customer joe = new Customer(UUID.randomUUID().toString(), "joe@example.com", "Joe", new Address("Cool Street", "Lisbon"));
    Customer jane = new Customer(UUID.randomUUID().toString(), "jane@example.com", "Jane", new Address("Cool Street", "Faro"));

    addCustomer(johanna);
    addCustomer(joe);
    addCustomer(jane);

    await()
        .ignoreExceptions()
        .atMost(10, TimeUnit.of(SECONDS))
        .until(
            () ->
                webClient
                    .get()
                    .uri("/wrapped/by_city?cities=Porto&cities=Lisbon")
                    .retrieve()
                    .bodyToMono(CustomersResponse.class)
                    .block(timeout),
            (res) -> res.customers.size() == 2 && // jane is from Faro
                res.customers.stream().allMatch(
                    c -> Arrays.asList(johanna.customerId(), joe.customerId()).contains(c.customerId()))
            );
  }

  private void addCustomer(Customer customer) {
    ResponseEntity<String> response =
        webClient.post()
            .uri("/customer/" + customer.customerId() + "/create")
            .bodyValue(customer)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
  }

}
