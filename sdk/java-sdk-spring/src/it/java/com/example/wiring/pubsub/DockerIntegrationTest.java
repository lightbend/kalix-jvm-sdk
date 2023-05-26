package com.example.wiring.pubsub;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kalix.devtools.impl.DockerComposeUtils;
import kalix.javasdk.JsonSupport;
import kalix.spring.impl.KalixSpringApplication;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import scala.jdk.FutureConverters;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public abstract class DockerIntegrationTest {
  protected WebClient webClient;
  protected Duration timeout = Duration.of(5, SECONDS);

  private DockerComposeUtils dockerComposeUtils = new DockerComposeUtils("docker-compose-integration.yml");

  private KalixSpringApplication kalixSpringApplication;

  public DockerIntegrationTest(ApplicationContext applicationContext) {
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
    //http://localhost:59644/counter/helloRestart/increaseaaaa/15 [DefaultWebClient]
    webClient = createClient("http://localhost:9000");
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
      .atMost(60, TimeUnit.SECONDS)
      .until(() -> assertSourceServiceIsUp(webClient),
        new IsEqual(HttpStatus.NOT_FOUND)  // NOT_FOUND is a sign that the customer registry service is there
      );

    return webClient;
  }
}
