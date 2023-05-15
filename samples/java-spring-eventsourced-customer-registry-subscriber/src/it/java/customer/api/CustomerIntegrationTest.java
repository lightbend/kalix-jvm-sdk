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
import scala.jdk.FutureConverters;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * This test exercises the integration between the current service (customer-registry-subscriber) and the customer-registry service.
 * <p>
 * The customer registry service is started as a docker container as well as it own kalix proxy. The current service is
 * started as a local JVM process (not dockerized), but its own kalix proxy starts as a docker container.
 * The `docker-compose-integration.yml` file is used to start all these services.
 * <p>
 * The subscriber service will first create a customer on customer-registry service. The customer will be streamed back
 * to the subscriber service and update its view.
 * <p>
 * This test will exercise the following:
 * - service under test can read settings from docker-compose file and correctly configure itself.
 * - resolution of service port mappings from docker-compose file allows for cross service calls (eg: create customer from subscriber service)
 * - resolution of service port mappings passed to kalix-proxy allows for service to service streaming (eg: customer view is updated in subscriber service)
 */
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
    confMap.put("kalix.user-function-interface", "0.0.0.0");

    // read service-port-mappings and pass to UF
    dockerComposeUtils.getLocalServicePortMappings().forEach(entry -> {
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
  public void afterAll() throws ExecutionException, InterruptedException {
    var kalixAppDown =
      new FutureConverters.FutureOps<>(kalixSpringApplication.stop())
        .asJava()
        .toCompletableFuture();

    var dockerDown = CompletableFuture.runAsync(() -> dockerComposeUtils.stopAndWait());
    CompletableFuture.allOf(kalixAppDown, dockerDown).get();
  }

  private HttpStatusCode assertSourceServiceIsUp(WebClient webClient) {
    try {
      return webClient.get()
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
          Mono.empty()
        )
        .toBodilessEntity()
        .block(timeout)
        .getStatusCode();

    } catch (WebClientRequestException ex) {
      throw new RuntimeException("This test requires an external kalix service to be running on localhost:9000 but was not able to reach it.");
    }
  }

  /* create the client but only return it after verifying that service is reachable */
  private WebClient createClient(String url) {

    var webClient =
      WebClient
        .builder()
        .baseUrl(url)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .codecs(configurer ->
          configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(JsonSupport.getObjectMapper(), MediaType.APPLICATION_JSON)
          )
        )
        .build();

    // wait until customer service is up
    await()
      .ignoreExceptions()
      .pollInterval(5, TimeUnit.SECONDS)
      .atMost(5, TimeUnit.MINUTES)
      .until(() -> assertSourceServiceIsUp(webClient),
        new IsEqual(HttpStatus.NOT_FOUND)  // NOT_FOUND is a sign that the customer registry service is there
      );

    return webClient;
  }


  /**
   * This test relies on a source Kalix service to which it subscribes. Such service should be running on :9000
   */
  @Test
  public void create() throws InterruptedException {

    createClient("http://localhost:9000");
    WebClient localWebClient = createClient("http://localhost:9001");

    // start the real test now  
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
