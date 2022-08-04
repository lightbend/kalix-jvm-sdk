package customer.api;


import customer.Main;
import kalix.springsdk.KalixConfigurationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class CustomerIntegrationTest {


  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);

  @Test
  public void create() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer(id, "foo@example.com", "Johanna", null);

    ResponseEntity<String> response =
        webClient.post()
        .uri("/customer/" + id + "/create")
        .bodyValue(customer)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals("Johanna", getCustomerById(id).name);
  }

  @Test
  public void changeName() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer(id, "foo@example.com", "Johanna", null);

    ResponseEntity<String> resCreation =
    webClient.post()
        .uri("/customer/" + id + "/create")
        .body(Mono.just(customer), Customer.class)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, resCreation.getStatusCode());

    ResponseEntity<String> resUpdate =
        webClient.post()
        .uri("/customer/" + id + "/changeName/" + "Katarina")
        .retrieve()
        .toEntity(String.class)
        .block(timeout);


    Assertions.assertEquals(HttpStatus.OK, resUpdate.getStatusCode());
    Assertions.assertEquals("Katarina", getCustomerById(id).name);
  }

  @Test
  public void changeAddress() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer(id, "foo@example.com", "Johanna", null);

    ResponseEntity<String> resCreation =
        webClient.post()
            .uri("/customer/" + id + "/create")
            .body(Mono.just(customer), Customer.class)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, resCreation.getStatusCode());

    Address address = new Address("Elm st. 5", "New Orleans");
    ResponseEntity<String> resUpdate =
        webClient.post()
            .uri("/customer/" + id + "/changeAddress")
            .body(Mono.just(address), Address.class)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);


    Assertions.assertEquals(HttpStatus.OK, resUpdate.getStatusCode());
    Assertions.assertEquals("Elm st. 5", getCustomerById(id).address.street);
  }


  private Customer getCustomerById(String customerId) {
    return webClient
        .get()
        .uri("/customer/" + customerId)
        .retrieve()
        .bodyToMono(Customer.class)
        .block(timeout);
  }

}
