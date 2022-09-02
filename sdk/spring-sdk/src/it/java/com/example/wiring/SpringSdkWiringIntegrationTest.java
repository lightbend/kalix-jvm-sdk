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
import kalix.springsdk.KalixConfigurationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class SpringSdkWiringIntegrationTest {

  @Autowired private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);

  @Test
  public void verifyEchoActionWiring() {

    Message response =
        webClient
            .get()
            .uri("/echo/message/abc")
            .retrieve()
            .bodyToMono(Message.class)
            .block(timeout);

    Assertions.assertEquals("Parrot says: 'abc'", response.text);
  }

  @Test
  public void verifyStreamActions() {

    List<Message> messageList =
        webClient
            .get()
            .uri("/echo/repeat/abc/times/3")
            .retrieve()
            .bodyToFlux(Message.class)
            .toStream()
            .collect(Collectors.toList());

    Assertions.assertEquals(3, messageList.size());
  }

  @Test
  public void verifyCounterEventSourcedWiring() {

    String counterIncrease =
        webClient
            .post()
            .uri("/counter/hello/increase/10")
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);

    Assertions.assertEquals("\"10\"", counterIncrease);

    String counterMultiply =
        webClient
            .post()
            .uri("/counter/hello/multiply/20")
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);

    Assertions.assertEquals("\"200\"", counterMultiply);

    String counterGet =
        webClient.get().uri("/counter/hello").retrieve().bodyToMono(String.class).block(timeout);

    Assertions.assertEquals("\"200\"", counterGet);
  }
}
