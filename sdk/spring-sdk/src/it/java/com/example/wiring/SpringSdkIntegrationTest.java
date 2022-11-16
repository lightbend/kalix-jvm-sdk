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

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class SpringSdkIntegrationTest {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

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
  public void verifyCounterEventSourceSubscription() {
    // GIVEN IncreaseAction is subscribed to CounterEntity events
    // WHEN the CounterEntity is requested to increase 42
    webClient
        .post()
        .uri("/counter/hello1/increase/42")
        .retrieve()
        .bodyToMono(Integer.class)
        .block(timeout);

    // THEN IncreaseAction receives the event 42 and increases the counter 1 more
    await()
        .ignoreExceptions()
        .atMost(10, TimeUnit.of(SECONDS))
        .until(
            () ->
                webClient
                    .get()
                    .uri("/counter/hello1")
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block(timeout),
            new IsEqual(42 + 1));
  }

  @Test
  public void verifyActionIsNotSubscribedToMultiplyAndRouterIgnores() {

    webClient
            .post()
            .uri("counter/counterId2/increase/1")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);

    webClient
            .post()
            .uri("counter/counterId2/multiply/2")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);


    Integer lastKnownValue =
        webClient
            .post()
            .uri("counter/counterId2/increase/1234")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);

    Assertions.assertEquals(1 * 2 + 1234, lastKnownValue);

    //Once the action IncreaseActionWithIgnore processes event 1234 it adds 1 more to the counter
    await()
            .atMost(10, TimeUnit.SECONDS)
            .until(
                    () ->
                            webClient
                                    .get()
                                    .uri("/counter/counterId2")
                                    .retrieve()
                                    .bodyToMono(Integer.class)
                                    .block(timeout),
                    new IsEqual<Integer>(1 * 2 + 1234 + 1 ));
  }

  @Test
  public void verifyViewIsNotSubscribedToMultiplyAndRouterIgnores() {

    webClient
            .post()
            .uri("counter/counterId4/increase/1")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);
    webClient
            .post()
            .uri("counter/counterId4/multiply/2")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);

    Integer counterGet = webClient
            .post()
            .uri("counter/counterId4/increase/1")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);

    Assertions.assertEquals(1 * 2 + 1, counterGet);

    await()
            .ignoreExceptions()
            .atMost(10, TimeUnit.SECONDS)
            .until(
                    () -> webClient
                            .get()
                            .uri("/counters-ignore/by-value-with-ignore/2")
                            .retrieve()
                            .bodyToMono(Counter.class)
                            .map(counter -> counter.value)
                            .block(timeout),
                    new IsEqual(1 + 1));
  }

  @Test
  public void verifyCounterEventSourcedWiring() {

    Integer counterIncrease =
        webClient
            .post()
            .uri("/counter/hello/increase/10")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);

    Assertions.assertEquals(10, counterIncrease);

    Integer counterMultiply =
        webClient
            .post()
            .uri("/counter/hello/multiply/20")
            .retrieve()
            .bodyToMono(Integer.class)
            .block(timeout);

    Assertions.assertEquals(200, counterMultiply);

    String counterGet =
        webClient.get().uri("/counter/hello").retrieve().bodyToMono(String.class).block(timeout);

    Assertions.assertEquals("\"200\"", counterGet);
  }

  @Test
  public void verifyFindCounterByValue() {

    ResponseEntity<String> response =
        webClient
            .post()
            .uri("/counter/abc/increase/10")
            .retrieve()
            .toEntity(String.class)
            .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

    // the view is eventually updated
    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(
            () ->
                webClient
                    .get()
                    .uri("/counters/by-value/10")
                    .retrieve()
                    .bodyToMono(Counter.class)
                    .map(counter -> counter.value)
                    .block(timeout),
            new IsEqual<Integer>(10));
  }

  @Test
  public void verifyCounterViewMultipleSubscriptions() throws InterruptedException {
    ResponseEntity<Integer> response1 =
        webClient
            .post()
            .uri("/counter/hello2/increase/1")
            .retrieve()
            .toEntity(Integer.class)
            .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response1.getStatusCode());
    ResponseEntity<Integer> response2 =
        webClient
            .post()
            .uri("/counter/hello3/increase/1")
            .retrieve()
            .toEntity(Integer.class)
            .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response2.getStatusCode());

    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.SECONDS)
        .until(
            () ->
                webClient
                    .get()
                    .uri("/counters-ms/by-value/1")
                    .retrieve()
                    .bodyToFlux(Counter.class)
                    .toStream()
                    .collect(Collectors.toList())
                    .size(),
            new IsEqual<>(2));
  }

  @Test
  public void verifyTransformedUserViewWiring() throws InterruptedException {

    TestUser user = new TestUser("123", "john@doe.com", "JohnDoe");

    createUser(user);
    updateUser(user.withName("JohnDoeJr"));


    // the view is eventually updated
    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(() -> getUserByEmail(user.email).version,
            new IsEqual(2));
  }

  @Test
  public void verifyUserSubscriptionAction() throws InterruptedException {

    TestUser user = new TestUser("123", "john@doe.com", "JohnDoe");

    createUser(user);

    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(() -> UserSideEffect.getUsers().get(user.id),
            new IsEqual(new User(user.email, user.name)));

    deleteUser(user);

    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(() -> UserSideEffect.getUsers().get(user.id),
            new IsNull<>());
  }


  @Test
  public void shouldDeleteValueEntityAndDeleteViewsState() throws InterruptedException {

    TestUser user = new TestUser("userId", "john2@doe.com", "Bob");
    createUser(user);

    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(() -> getUserByEmail(user.email).version,
            new IsEqual(1));

    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(() -> getUsersByName(user.name).size(),
            new IsEqual(1));

    deleteUser(user);

    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(
            () ->
                webClient
                    .get()
                    .uri("/users/by-email/" + user.email)
                    .exchangeToMono(clientResponse -> Mono.just(clientResponse.statusCode()))
                    .block(timeout)
                    .value(),
            new IsEqual(404));

    await()
        .ignoreExceptions()
        .atMost(15, TimeUnit.of(SECONDS))
        .until(() -> getUsersByName(user.name).size(),
            new IsEqual(0));
  }

  @Test
  public void verifyFindUsersByEmail() {

    TestUser user = new TestUser("JohnDoe", "john3@doe.com", "JohnDoe");
    createUser(user);

    // the view is eventually updated
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.SECONDS)
        .until(
            () ->
                webClient
                    .get()
                    .uri("/users/by_email/" + user.email)
                    .retrieve()
                    .bodyToMono(User.class)
                    .block()
                    .email,
            new IsEqual(user.email));
  }

  @Test
  public void verifyFindUsersByNameStreaming() {

    TestUser joe1 = new TestUser("user1", "john@doe.com", "joe");
    TestUser joe2 = new TestUser("user2", "john@doe.com", "joe");
    createUser(joe1);
    createUser(joe2);

    // the view is eventually updated
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.SECONDS)
        .until(() -> getUsersByName("joe").size(),
            new IsEqual(2));
  }

  @NotNull
  private List<User> getUsersByName(String name) {
    return webClient
        .get()
        .uri("/users/by-name/"+name)
        .retrieve()
        .bodyToFlux(User.class)
        .toStream()
        .collect(Collectors.toList());
  }

  @Nullable
  private UserWithVersion getUserByEmail(String email) {
    return webClient
        .get()
        .uri("/users/by-email/" + email)
        .retrieve()
        .bodyToMono(UserWithVersion.class)
        .block(timeout);
  }

  private void updateUser(TestUser user) {
    String userUpdate =
        webClient
            .post()
            .uri("/user/" + user.id + "/" + user.email + "/" + user.name)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok\"", userUpdate);
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

  private void deleteUser(TestUser user) {
    String deleteUser =
        webClient
            .delete()
            .uri("/user/" + user.id)
            .retrieve()
            .bodyToMono(String.class)
            .block(timeout);
    Assertions.assertEquals("\"Ok from delete\"", deleteUser);
  }
}

class TestUser {
  public final String id;
  public final String email;
  public final String name;

  public TestUser(String id, String email, String name) {
    this.id = id;
    this.email = email;
    this.name = name;
  }

  public TestUser withName(String newName) {
    return new TestUser(id, email, newName);
  }
}
