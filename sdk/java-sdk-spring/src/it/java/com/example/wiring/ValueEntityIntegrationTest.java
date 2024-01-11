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
import com.example.wiring.eventsourcedentities.counter.CounterEntity;
import com.example.wiring.valueentities.user.User;
import kalix.spring.KalixConfigurationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.ClientResponse;
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
public class ValueEntityIntegrationTest {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void verifyValueEntityCurrentState() {

    var joe1 = new TestUser("veUser1", "john@doe.com", "veJane");
    createUser(joe1);

    var newEmail = joe1.email + "2";
    // change email uses the currentState internally
    changeEmail(joe1.withEmail(newEmail));

    Assertions.assertEquals(newEmail, getUser(joe1).email);
  }

  @Test
  public void failRequestWhenReqParamsIsNotPresent() {

    var joe1 = new TestUser("veUser1", "john@doe.com", "veJane");
    createUser(joe1);

    ResponseEntity<String> response =
      webClient
        .patch()
        .uri("/user/" + joe1.id + "/email")
        .retrieve()
        .toEntity(String.class)
        .onErrorResume(WebClientResponseException.class, error -> Mono.just(ResponseEntity.status(error.getStatusCode()).body(error.getResponseBodyAsString())))
        .block(timeout);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("Required request parameter is missing: email");
  }

  @Test
  public void verifyValueEntityCurrentStateAfterRestart() {

    var joe2 = new TestUser("veUser2", "veJane@doe.com", "veJane");
    createUser(joe2);

    restartUserEntity(joe2);

    var newEmail = joe2.email + "2";

    await()
        .ignoreExceptions()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          // change email uses the currentState internally
          changeEmail(joe2.withEmail(newEmail), Duration.ofMillis(1000));

          Assertions.assertEquals(newEmail, getUser(joe2).email);
        });
  }

  private void createUser(TestUser user) {
    ClientResponse response =
        webClient
            .post()
            .uri("/user/" + user.id + "/" + user.email + "/" + user.name)
            .exchangeToMono(Mono::just)
            .block(timeout);
    Assertions.assertEquals(201, response.statusCode().value());
  }

  private void changeEmail(TestUser user) {
    changeEmail(user, timeout);
  }

  private void changeEmail(TestUser user, Duration timeout) {
    String userCreation =
        webClient
            .patch()
            .uri("/user/" + user.id + "/email/" + user.email)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok from patch\"", userCreation);
  }

  private User getUser(TestUser user) {
    return webClient
        .get()
        .uri("/user/" + user.id)
        .retrieve()
        .bodyToMono(User.class)
        .block(timeout);
  }

  private void restartUserEntity(TestUser user) {
    try {
      webClient
          .post()
          .uri("/user/" + user.id +"/restart")
          .retrieve()
          .bodyToMono(Integer.class)
          .block(timeout);
      fail("This should not be reached");
    } catch (Exception ignored) { }
  }
}
