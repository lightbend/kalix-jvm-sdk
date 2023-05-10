package customer.api;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import customer.Main;
import customer.actions.CustomerRegistryAction;
import customer.views.Customer;
import kalix.devtools.impl.DockerComposeUtils;
import kalix.javasdk.JsonSupport;
import kalix.spring.impl.KalixSpringApplication;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = Main.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerIntegrationTest {

  final private Duration timeout = Duration.of(5, SECONDS);

  private DockerComposeUtils dockerComposeUtils = new DockerComposeUtils("docker-compose-integration.yml");

  private KalixSpringApplication kalixSpringApplication;

  public CustomerIntegrationTest(ApplicationContext applicationContext) {
    Map<String, Object> confMap = new HashMap<>();
    confMap.put("kalix.user-function-port", dockerComposeUtils.userFunctionPort());
    // don't kill the test JVM when terminating the KalixRunner
    confMap.put("kalix.system.akka.coordinated-shutdown.exit-jvm", "off");
    // dev-mode should be false when running integration tests
    confMap.put("kalix.dev-mode.enabled", false);

    // read service-port-mappings and pass to UF
    dockerComposeUtils.getServicePortMappings().forEach(entry -> {
        var split = entry.replace("-D", "").split("=");
        confMap.put(split[0], split[1]);
      }
    );

    Config config = ConfigFactory.parseMap(confMap).withFallback(ConfigFactory.load());

    kalixSpringApplication = new KalixSpringApplication(applicationContext, config);
  }

  @BeforeAll
  public void beforeAll() {
    dockerComposeUtils.start();
    kalixSpringApplication.start();
  }

  @AfterAll
  public void afterAll() {
    kalixSpringApplication.stop();
    dockerComposeUtils.stop();
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

  private WebClient createClient(String url) {
    return WebClient
      .builder()
      .baseUrl(url)
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .codecs(configurer ->
        configurer.defaultCodecs().jackson2JsonEncoder(
          new Jackson2JsonEncoder(JsonSupport.getObjectMapper(), MediaType.APPLICATION_JSON)
        )
      )
      .build();
  }


  /**
   * This test relies on a source Kalix service to which it subscribes. Such service should be running on :9000
   */
  @Test
  public void create() throws InterruptedException {

    WebClient customerRegistryService = createClient("http://host.docker.internal:9000");

    // wait until customer service is up
    await()
      .ignoreExceptions()
      .pollInterval(5, TimeUnit.SECONDS)
      .atMost(5, TimeUnit.MINUTES)
      .until(() -> assertSourceServiceIsUp(customerRegistryService),
        new IsEqual(HttpStatus.NOT_FOUND)  // NOT_FOUND is a sign that the source service is there
      );


    WebClient localWebClient = createClient("http://host.docker.internal:9001");

    String id = UUID.randomUUID().toString();
    Customer customer = new Customer("Johanna", "foo@example.com", "Johanna");

    ResponseEntity<CustomerRegistryAction.Confirm> response =
      localWebClient.post()
        .uri("/customer/" + id + "/create")
        .bodyValue(customer)
        .retrieve()
        .toEntity(CustomerRegistryAction.Confirm.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());


    // the view is eventually updated
    // on this service (updated via s2s streaming)
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
