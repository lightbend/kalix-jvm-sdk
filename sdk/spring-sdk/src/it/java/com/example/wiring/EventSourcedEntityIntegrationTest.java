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

package com.example.wiring;

import com.example.Main;
import kalix.springsdk.KalixConfigurationTest;
import org.hamcrest.core.IsEqual;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class EventSourcedEntityIntegrationTest {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Ignore
  public void verifyCounterEventSourcedWiring() {

    var counterId = "hello";

    Integer counterIncrease = increaseCounter(counterId, 10);
    Assertions.assertEquals(10, counterIncrease);

    Integer counterMultiply = multiplyCounter(counterId, 20);
    Assertions.assertEquals(200, counterMultiply);

    int counterGet = getCounter(counterId);
    Assertions.assertEquals(200, counterGet);
  }

  @Ignore
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

  @Ignore
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
      .atMost(Duration.ofSeconds(20))
      .until(
        () -> getCounter(counterId),
        new IsEqual(10));
  }

  private Integer increaseCounter(String name, int value) {
    return webClient
        .post()
        .uri("/counter/"+ name +"/increase/" + value)
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