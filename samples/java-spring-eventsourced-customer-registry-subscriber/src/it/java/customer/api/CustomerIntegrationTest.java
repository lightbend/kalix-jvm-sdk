package customer.api;


import customer.Main;
import customer.views.Customer;
import kalix.devtools.impl.DockerComposeUtils;
import kalix.javasdk.JsonSupport;
import kalix.spring.impl.KalixSpringApplication;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;


@SpringBootTest(classes = Main.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(ExtraKalixConfiguration.class)
@TestPropertySource(properties =
  "spring.main.allow-bean-definition-overriding=true"
)
public class CustomerIntegrationTest {

  final private Duration timeout = Duration.of(5, SECONDS);

  final private WebClient localWebClient;

  @Autowired
  private DockerComposeUtils dockerComposeUtils;

  @Autowired
  private KalixSpringApplication kalixSpringApplication;

  public CustomerIntegrationTest() {

    localWebClient =
      WebClient
        .builder()
        // TODO: extend DockerComposeUtils to provide proxy port
        .baseUrl("http://localhost:9000")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .codecs(configurer ->
          configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(JsonSupport.getObjectMapper(), MediaType.APPLICATION_JSON)
          )
        )
        .build();
  }

  @BeforeAll
  public void beforeAll() throws IOException {
//    dockerComposeUtils.start();
  }

  @AfterAll
  public void afterAll() {
//    kalixSpringApplication.stop();
//    dockerComposeUtils.stop();
  }

  private HttpStatusCode assertSourceServiceIsUp(WebClient sourceServiceWebClient) {
    try {
      return sourceServiceWebClient.get()
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
          Mono.empty()
        )
        .toBodilessEntity()
        .block(timeout)
        .getStatusCode();

    } catch (WebClientRequestException ex) {
      throw new RuntimeException("This test requires an external kalix service to be running on localhost:9001 but was not able to reach it.");
    }
  }

  /**
   * This test relies on a source Kalix service to which it subscribes. Such service should be running on :9000
   */
  @Test
  public void create() {
    WebClient sourceServiceWebClient = WebClient.builder().baseUrl("http://localhost:9001")
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build();

    // wait until customer service is up
    await()
      .ignoreExceptions()
      .atMost(20, TimeUnit.SECONDS)
      .until(() -> assertSourceServiceIsUp(sourceServiceWebClient),
        new IsEqual(HttpStatus.NOT_FOUND)  // NOT_FOUND is a sign that the source service is there
      );

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
          localWebClient.get()
            .uri("/customers/by_name/Johanna")
            .retrieve()
            .bodyToFlux(Customer.class)
            .blockFirst()
            .name(),
        new IsEqual("Johanna")
      );
  }

}
