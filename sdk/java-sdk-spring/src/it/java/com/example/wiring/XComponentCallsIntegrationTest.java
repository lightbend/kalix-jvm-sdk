/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring;

import com.example.Main;
import com.example.wiring.actions.echo.Message;
import com.example.wiring.valueentities.user.User;
import kalix.spring.KalixConfigurationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

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
    Assertions.assertEquals("Parrot says: 'mssg t b shrtnd'", response.text());
  }

  @Test
  public void verifyEchoActionXComponentCallUsingRequestParam() {

    Message usingGetResponse =
            webClient
                    .get()
                    .uri(builder -> builder.path("/echo/message-short")
                            .queryParam("msg", "message to be shortened")
                            .build())
                    .retrieve()
                    .bodyToMono(Message.class)
                    .block(timeout);

    Assertions.assertNotNull(usingGetResponse);
    Assertions.assertEquals("Parrot says: 'm3ss4g3 t b3 shrt3n3d'", usingGetResponse.text());

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
    Assertions.assertEquals("Parrot says: 'm3ss4g3 t b3 shrt3n3d'", usingGetResponse.text());

    Message usingPostResponse =
            webClient
                    .post()
                    .uri("/echo/message/leetshort")
                    .bodyValue(new Message("message to be shortened"))
                    .retrieve()
                    .bodyToMono(Message.class)
                    .block(timeout);

    Assertions.assertNotNull(usingPostResponse);
    Assertions.assertEquals("Parrot says: 'm3ss4g3 t b3 shrt3n3d'", usingPostResponse.text());
  }

  @Test
  public void verifyKalixClientUsingPutMethod() {

    User u1 = new User("mary@pops.com", "MayPops");
    String userCreation =
        webClient
            .put()
            .uri("/validuser/MaryPops/" + u1.email + "/" + u1.name)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok from put\"", userCreation);
  }

  @Test
  public void verifyKalixClientUsingPatchMethod() {

    User u1 = new User("mary@patch.com", "MayPatch");
    String userCreation =
        webClient
            .put()
            .uri("/validuser/MayPatch/" + u1.email + "/" + u1.name)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok from put\"", userCreation);

    String userUpdate =
        webClient
            .patch()
            .uri("/validuser/MayPatch/email/" + "new"+u1.email)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok from patch\"", userUpdate);

    User userGetResponse =
        webClient
            .get()
            .uri("/user/MayPatch")
            .retrieve()
            .bodyToMono(User.class)
            .block(timeout);
    Assertions.assertEquals("new"+u1.email, userGetResponse.email);
  }

  @Test
  public void verifyKalixClientUsingDeleteMethod() {

    User u1 = new User("mary@delete.com", "MayDelete");
    String userCreation =
        webClient
            .put()
            .uri("/validuser/MayDelete/" + u1.email + "/" + u1.name)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok from put\"", userCreation);

    User userGetResponse =
        webClient
            .get()
            .uri("/user/MayDelete")
            .retrieve()
            .bodyToMono(User.class)
            .block(timeout);
    Assertions.assertEquals(u1.email, userGetResponse.email);

    String userDelete =
        webClient
            .delete()
            .uri("/validuser/MayDelete")
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok from delete\"", userDelete);

    var userGetResponse2 =
        webClient
            .get()
            .uri("/user/MayDelete")
            .exchangeToMono(clientResponse -> Mono.just(clientResponse.statusCode()))
            .block(timeout);
    Assertions.assertEquals(404, userGetResponse2.value());
  }
}
