/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wiring.pubsub;

import com.example.Main;
import com.example.wiring.eventsourcedentities.counter.CounterEvent;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kalix.devtools.impl.DockerComposeUtils;
import kalix.javasdk.JsonSupport;
import kalix.spring.impl.KalixSpringApplication;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import scala.jdk.FutureConverters;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = Main.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("docker-it-test")
public class PubSubIntegrationTest {

  private WebClient webClient;

  final private Duration timeout = Duration.of(5, SECONDS);

  private DockerComposeUtils dockerComposeUtils = new DockerComposeUtils("docker-compose-integration.yml");

  private KalixSpringApplication kalixSpringApplication;

  public PubSubIntegrationTest(ApplicationContext applicationContext) {
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

  @Test
  public void shouldVerifyActionSubscribingToTopic() throws InterruptedException {
    //given
    String counterId = "some-counter";

    //when
    increaseCounter(counterId, 2);
    increaseCounter(counterId, 2);
    multiplyCounter(counterId, 10);

    //then
    await()
      .ignoreExceptions()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var response = webClient
          .get()
          .uri("/subscribe-to-topic/" + counterId)
          .retrieve()
          .bodyToMono(List.class)
          .block(timeout);


        assertThat(response).hasSize(3);
      });
  }

  @Test
  public void shouldVerifyViewSubscribingToTopic() throws InterruptedException {
    //given
    String counterId1 = "some-counter-1";
    String counterId2 = "some-counter-2";

    //when
    increaseCounter(counterId1, 2);
    increaseCounter(counterId1, 2);
    multiplyCounter(counterId1, 10);
    increaseCounter(counterId2, 2);
    multiplyCounter(counterId2, 10);

    //then
    await()
      .ignoreExceptions()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var response = webClient
          .get()
          .uri("/counter-view-topic-sub/less-then/" + 30)
          .retrieve()
          .bodyToFlux(CounterView.class)
          .toStream()
          .toList();

        assertThat(response).containsOnly(new CounterView(counterId2, 20));
      });
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

  private Integer increaseCounter(String name, int value) {
    return webClient
      .post()
      .uri("/counter/" + name + "/increase/" + value)
      .retrieve()
      .bodyToMono(Integer.class)
      .block(timeout);
  }

  private Integer multiplyCounter(String name, int value) {
    return webClient
      .post()
      .uri("/counter/" + name + "/multiply/" + value)
      .retrieve()
      .bodyToMono(Integer.class)
      .block(timeout);
  }
}
