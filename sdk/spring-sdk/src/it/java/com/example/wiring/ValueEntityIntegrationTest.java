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
import com.example.wiring.valueentities.user.User;
import kalix.springsdk.KalixConfigurationTest;
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
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class ValueEntityIntegrationTest {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Ignore
  public void verifyValueEntityCurrentState() {

    var joe1 = new TestUser("veUser1", "john@doe.com", "veJane");
    createUser(joe1);

    var newEmail = joe1.email + "2";
    // change email uses the currentState internally
    changeEmail(joe1.withEmail(newEmail));

    Assertions.assertEquals(newEmail, getUser(joe1).email);
  }

  @Ignore
  public void verifyValueEntityCurrentStateAfterRestart() {

    var joe2 = new TestUser("veUser2", "veJane@doe.com", "veJane");
    createUser(joe2);

    restartUserEntity(joe2);

    var newEmail = joe2.email + "2";
    // change email uses the currentState internally
    changeEmail(joe2.withEmail(newEmail));

    Assertions.assertEquals(newEmail, getUser(joe2).email);
  }

  private void createUser(TestUser user) {
    String userCreation =
        webClient
            .post()
            .uri("/user/" + user.id + "/" + user.email + "/" + user.name)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok\"", userCreation);
  }

  private void changeEmail(TestUser user) {
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
