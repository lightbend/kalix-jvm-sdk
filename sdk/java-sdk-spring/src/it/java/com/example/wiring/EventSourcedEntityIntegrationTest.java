/*
 * Copyright 2024 Lightbend Inc.
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

package com.example.wiring;

import com.example.Main;
import kalix.spring.KalixConfigurationTest;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class EventSourcedEntityIntegrationTest {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void verifyCounterEventSourcedWiring() {

    var counterId = "hello";

    Integer counterIncrease = increaseCounter(counterId, 10);
    Assertions.assertEquals(10, counterIncrease);

    Integer counterMultiply = multiplyCounter(counterId, 20);
    Assertions.assertEquals(200, counterMultiply);

    int counterGet = getCounter(counterId);
    Assertions.assertEquals(200, counterGet);
  }

  @Test
  public void verifyCounterEventSourcedAfterRestart() {

    var counterId = "helloRestart";

    increaseCounter(counterId, 15);
    multiplyCounter(counterId, 2);
    int counterGet = getCounter(counterId);
    Assertions.assertEquals(30, counterGet);

    // force restart of counter entity
    restartCounterEntity(counterId);

    // events should be replayed successfully and
    // counter value should be the same as previously
    int counterGet2 = getCounter(counterId);
    Assertions.assertEquals(30, counterGet2);
  }

  @Test
  public void verifyCounterEventSourcedAfterRestartFromSnapshot() {

    // snapshotting with kalix.event-sourced-entity.snapshot-every = 10
    var counterId = "restartFromSnapshot";

    // force the entity to snapshot
    for (int i = 0; i < 10; i++) {
      increaseCounter(counterId, 1);
    }
    Assertions.assertEquals(10, getCounter(counterId));

    // force restart of counter entity
    restartCounterEntity(counterId);

    // current state is based on snapshot and should be the same as previously
    await()
      .ignoreExceptions()
      .atMost(20, TimeUnit.of(SECONDS))
      .until(
        () -> getCounter(counterId),
        new IsEqual(10));
  }

  @Test
  public void verifyRequestWithDefaultProtoValuesWithEntity() {
    var counterId = "some-counter";

    increaseCounter(counterId, 2);
    Integer result = webClient
      .post()
      .uri("/counter/" + counterId + "/set/" + 0)
      .retrieve()
      .bodyToMono(Integer.class)
      .block(timeout);

    assertThat(result).isEqualTo(0);
  }

  @Test
  public void verifyRequestWithDefaultProtoValuesWithEntityByReqParams() {
    var counterId = "some-counter";

    increaseCounter(counterId, 2);
    Integer result = webClient
      .post()
      .uri("/counter/" + counterId + "/set?value=" + 0)
      .retrieve()
      .bodyToMono(Integer.class)
      .block(timeout);

    assertThat(result).isEqualTo(0);
  }

  @Test
  public void failRequestWhenReqParamsIsNotPresent() {
    var counterId = "some-counter";

    increaseCounter(counterId, 2);
    ResponseEntity<String> result = webClient
      .post()
      .uri("/counter/" + counterId + "/set")
      .retrieve()
      .toEntity(String.class)
      .onErrorResume(WebClientResponseException.class, error -> Mono.just(ResponseEntity.status(error.getStatusCode()).body(error.getResponseBodyAsString())))
      .block(timeout);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(result.getBody()).isEqualTo("Required request parameter is missing: value");
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
        .uri("/counter/"+ name +"/multiply/" + value)
        .retrieve()
        .bodyToMono(Integer.class)
        .block(timeout);
  }

  private void restartCounterEntity(String name) {
    try {
      webClient
          .post()
          .uri("/counter/" + name +"/restart")
          .retrieve()
          .bodyToMono(Integer.class)
          .block(timeout);
      fail("This should not be reached");
    } catch (Exception ignored) { }
  }

  private Integer getCounter(String name) {
    var result = webClient.get().uri("/counter/" + name).retrieve().bodyToMono(String.class).block(timeout);
    return Integer.valueOf(result.replaceAll("\"","")); // transforming ""1"" -> "1" -> 1
  }
}