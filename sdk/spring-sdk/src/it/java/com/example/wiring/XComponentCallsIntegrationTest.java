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

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class XComponentCallsIntegrationTest {

  @Autowired private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void verifyEchoActionXComponentCall() {

    Message response =
        webClient
            .get()
            .uri("/echo/message/{msg}/short", "message to be shortened")
            .retrieve()
            .bodyToMono(Message.class)
            .block(timeout);

    Assertions.assertNotNull(response);
    Assertions.assertEquals("Parrot says: 'mssg t b shrtnd'", response.text);
  }

  @Test
  public void verifyEchoActionXComponentCallUsingRequestParam() {

    Message usingGetResponse =
            webClient
                    .get()
                    .uri(builder -> builder.path("/echo/message/short")
                            .queryParam("msg", "message to be shortened")
                            .build())
                    .retrieve()
                    .bodyToMono(Message.class)
                    .block(timeout);

    Assertions.assertNotNull(usingGetResponse);
    Assertions.assertEquals("Parrot says: 'm3ss4g3 t b3 shrt3n3d'", usingGetResponse.text);

  }

  @Test
  public void verifyEchoActionXComponentCallUsingForward() {

    Message usingGetResponse =
            webClient
                    .get()
                    .uri("/echo/message/{msg}/leetshort", "message to be shortened")
                    .retrieve()
                    .bodyToMono(Message.class)
                    .block(timeout);

    Assertions.assertNotNull(usingGetResponse);
    Assertions.assertEquals("Parrot says: 'm3ss4g3 t b3 shrt3n3d'", usingGetResponse.text);

    Message usingPostResponse =
            webClient
                    .post()
                    .uri("/echo/message/leetshort")
                    .bodyValue(new Message("message to be shortened"))
                    .retrieve()
                    .bodyToMono(Message.class)
                    .block(timeout);

    Assertions.assertNotNull(usingPostResponse);
    Assertions.assertEquals("Parrot says: 'm3ss4g3 t b3 shrt3n3d'", usingPostResponse.text);
  }
}
