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
import com.example.wiring.actions.echo.Message;
import com.example.wiring.eventsourcedentities.counter.Counter;
import com.example.wiring.valueentities.user.User;
import com.example.wiring.valueentities.user.UserSideEffect;
import com.example.wiring.views.UserWithVersion;
import kalix.springsdk.KalixConfigurationTest;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

  @Test
  public void verifyCounterEventSourcedWiring() {

    var counterId = "hello";

    Integer counterIncrease = increaseCounter(counterId, 10);
    Assertions.assertEquals(10, counterIncrease);

    Integer counterMultiply =
        webClient
            .post()
            .uri("/counter/" + counterId + "/multiply/20")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);

    Assertions.assertEquals(200, counterMultiply);

    int counterGet = getCounter(counterId);
    Assertions.assertEquals(200, counterGet);
  }

  @Test
  public void verifyCounterEventSourcedAfterRestart() {

    var counterId = "helloRestart";

    Integer counterIncrease = increaseCounter(counterId, 10);
    Assertions.assertEquals(10, counterIncrease);

    int counterGet = getCounter(counterId);
    Assertions.assertEquals(10, counterGet);

    try {
      webClient
          .post()
          .uri("/counter/" + counterId +"/restart")
          .retrieve()
          .bodyToMono(Integer.class)
          .block(timeout);
      fail("This should not be reached");
    } catch (Exception ignored) { }

    int counterGet2 = getCounter(counterId);
    Assertions.assertEquals(10, counterGet2);
  }

  private Integer increaseCounter(String name, int value) {
    return webClient
        .post()
        .uri("/counter/"+ name +"/increase/" + value)
        .retrieve()
        .bodyToMono(Integer.class)
        .block(timeout);
  }

  private Integer getCounter(String name) {
    var result = webClient.get().uri("/counter/" + name).retrieve().bodyToMono(String.class).block(timeout);
    return Integer.valueOf(result.replaceAll("\"","")); // transforming ""1"" -> "1" -> 1
  }
}