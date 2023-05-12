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
import kalix.devtools.impl.DockerComposeUtils;
import kalix.javasdk.KalixRunner;
import kalix.javasdk.impl.GrpcClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.hamcrest.core.IsEqual;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerIntegrationTest {

  final private Duration timeout = Duration.of(5, SECONDS);
  private final KalixRunner kalixRunner;

  private DockerComposeUtils dockerComposeUtils = new DockerComposeUtils("docker-compose-integration.yml");

  private final ActorSystem testSystem;

  public CustomerIntegrationTest() {
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
    this.kalixRunner = Main.createKalix().createRunner(config);
    this.testSystem = ActorSystem.create("test-system", config);
  }


  @BeforeAll
  public void beforeAll() {
    dockerComposeUtils.start();
    kalixRunner.run();
  }

  @AfterAll
  public void afterAll() throws ExecutionException, InterruptedException {
    testSystem.terminate();
    var actorSysDown = testSystem.getWhenTerminated();
    var kalixDown = kalixRunner.terminate();
    var dockerDown = CompletableFuture.runAsync(() -> dockerComposeUtils.stop());
    CompletableFuture.allOf(actorSysDown.toCompletableFuture(), kalixDown.toCompletableFuture(), dockerDown).get();
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