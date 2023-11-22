package customer;


import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import com.google.protobuf.Empty;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import customer.action.CustomerAction;
import customer.api.CustomerApi;
import customer.view.AllCustomersView;
import kalix.javasdk.KalixRunner;
import kalix.javasdk.impl.GrpcClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import org.hamcrest.core.IsEqual;
import static org.awaitility.Awaitility.await;

/**
 * This test exercises the integration between the current service (customer-registry-subscriber) and the customer-registry service.
 * <p>
 * The customer registry service is started as a Docker container as well as it own Kalix Runtime. The current service is
 * started as a local JVM process (not dockerized), but its own Kalix Runtime starts as a Docker container.
 * The `docker-compose-integration.yml` file is used to start all these services.
 * <p>
 * The subscriber service will first create a customer on customer-registry service. The customer will be streamed back
 * to the subscriber service and update its view.
 * <p>
 * This test will exercise the following:
 * - service under test can read settings from docker-compose file and correctly configure itself.
 * - resolution of service port mappings from docker-compose file allows for cross service calls (eg: create customer from subscriber service)
 * - resolution of service port mappings passed to kalix-runtime allows for service to service streaming (eg: customer view is updated in subscriber service)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerIntegrationTest {

  final private Duration timeout = Duration.of(5, SECONDS);
  private final KalixRunner kalixRunner;

  private final ActorSystem testSystem;

  public CustomerIntegrationTest() {
    Map<String, Object> confMap = new HashMap<>();
    // don't kill the test JVM when terminating the KalixRunner
    confMap.put("kalix.system.akka.coordinated-shutdown.exit-jvm", "off");
    confMap.put("kalix.dev-mode.docker-compose-file", "docker-compose-integration.yml");
    confMap.put("kalix.user-function-interface", "0.0.0.0");


    Config config = ConfigFactory.parseMap(confMap).withFallback(ConfigFactory.load());
    this.kalixRunner = Main.createKalix().createRunner(config);
    this.testSystem = ActorSystem.create("test-system");
  }


  @BeforeAll
  public void beforeAll() {
    kalixRunner.run();
  }

  @AfterAll
  public void afterAll() throws ExecutionException, InterruptedException {
    testSystem.terminate();
    var actorSysDown = testSystem.getWhenTerminated();
    var kalixAppDown = kalixRunner.terminate();
    CompletableFuture.allOf(actorSysDown.toCompletableFuture(), kalixAppDown.toCompletableFuture()).get();
  }

  public CustomerAction customerActionClient() {
    return GrpcClients.get(testSystem).getGrpcClient(CustomerAction.class, "localhost", 9001);
  }

  public AllCustomersView customersViewClient() {
    return GrpcClients.get(testSystem).getGrpcClient(AllCustomersView.class, "localhost", 9001);
  }


  /**
   * This test relies on a source Kalix service to which it subscribes. Such service should be running on :9000
   */
  @Test
  public void create() {

    var id = UUID.randomUUID().toString();

    // try until it succeeds
    await()
      .ignoreExceptions()
      .pollInterval(5, TimeUnit.SECONDS)
      .atMost(5, TimeUnit.MINUTES)
      .until(() ->
          customerActionClient().create(CustomerApi.Customer.newBuilder()
            .setCustomerId(id)
            .setName("Johanna")
            .setEmail("foo@example.com")
            .build()).toCompletableFuture().get(),
        new IsEqual<>(Empty.getDefaultInstance())
      );

    await()
      .ignoreExceptions()
      .pollInterval(2, TimeUnit.SECONDS)
      .atMost(20, TimeUnit.SECONDS)
      .until(() ->
          customersViewClient()
            .getCustomers(Empty.getDefaultInstance())
            .runWith(Sink.last(), testSystem).toCompletableFuture().get(),
        customer -> customer.getName().equals("Johanna")
      );

  }

}