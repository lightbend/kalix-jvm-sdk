package customer.api;


import customer.Main;
import customer.views.Customer;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import(TestkitConfig.class)
public class CustomerIntegrationTest extends KalixIntegrationTestKitSupport {



  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);

  private void assertSourceServiceIsUp(WebClient sourceServiceWebClient) {
    try {
      var code =
          sourceServiceWebClient.get()
              .retrieve()
              .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                  Mono.empty()
              )
              .toBodilessEntity()
              .block(timeout);;
      Assertions.assertEquals(HttpStatus.NOT_FOUND, code.getStatusCode()); // NOT_FOUND is a sign that the source service is there
    } catch (WebClientRequestException ex) {
      Assertions.fail("This test requires an external kalix service to be running on localhost:9000 but was not able to reach it.");
    }
  }

  /**
   * This test relies on a source Kalix service to which it subscribes. Such service should be running on :9000
   */
  @Test
  public void create() {
    WebClient sourceServiceWebClient = WebClient.builder().baseUrl("http://localhost:9000")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();

    assertSourceServiceIsUp(sourceServiceWebClient);

    String id = UUID.randomUUID().toString();
    Customer customer = new Customer("Johanna", "foo@example.com", "Johanna");

    ResponseEntity<String> response =
        sourceServiceWebClient.post()
            .uri("/customer/" + id + "/create")
            .bodyValue(customer)
            .retrieve()
            .toEntity(String.class)
            .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

    // the view is eventually updated
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.SECONDS)
        .until(() ->
                webClient.get()
                    .uri("/customers/by_name/Johanna")
                    .retrieve()
                    .bodyToFlux(Customer.class)
                    .blockFirst()
                    .name(),
            new IsEqual("Johanna")
        );
  }

}
