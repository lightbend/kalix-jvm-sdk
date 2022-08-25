package customer.view;

import customer.Main;
import customer.api.Customer;
import kalix.springsdk.KalixConfigurationTest;
import org.hamcrest.core.IsEqual;
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

import java.time.Duration;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class CustomerByNameIntegrationTest {


  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);


  @Test
  public void findByName() throws Exception {

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


    // the view is eventually updated
    await()
        .atMost(20, TimeUnit.SECONDS)
        .until(() ->
                webClient.get()
                    .uri("/customer/by_name/Johanna")
                    .retrieve()
                    .bodyToMono(Customer.class)
                    .block(timeout)
                    .name,
            new IsEqual("Johanna")
        );
  }
}
