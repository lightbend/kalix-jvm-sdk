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
import com.example.wiring.actions.headers.ForwardHeadersAction;
import com.example.wiring.eventsourcedentities.counter.Counter;
import com.example.wiring.valueentities.customer.CustomerEntity;
import com.example.wiring.valueentities.user.User;
import com.example.wiring.valueentities.user.UserSideEffect;
import com.example.wiring.views.CustomerByCreationTime;
import com.example.wiring.views.UserCounters;
import com.example.wiring.views.UserWithVersion;
import kalix.springsdk.KalixConfigurationTest;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import({KalixConfigurationTest.class, TestkitConfig.class})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class SpringSdkIntegrationTest {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  final private Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  public void verifyJavaPrimitivesAsParams() {

    Message response =
        webClient
            .get()
            .uri("/action/1.0/2.0/3/4?shortValue=5&byteValue=6&charValue=97&booleanValue=true")
            .retrieve()
            .bodyToMono(Message.class)
            .block(timeout);

    Assertions.assertEquals("1.02.03456atrue", response.text);

    Message responseCollections =
        webClient
            .get()
            .uri("/action_collections?ints=1&ints=2")
            .retrieve()
            .bodyToMono(Message.class)
            .block(timeout);

    Assertions.assertEquals("1,2", responseCollections.text);
  }

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
  public void verifyEchoActionRequestParam() {

    Message response =
        webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/echo/message")
                .queryParam("msg", "queryParam")
                .build())
            .retrieve()
            .bodyToMono(Message.class)
            .block(timeout);

    Assertions.assertEquals("Parrot says: 'queryParam'", response.text);

    var failedReq =
        webClient
            .get()
            .uri("/echo/message")
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
            .block(timeout);
    Assertions.assertTrue(failedReq.contains("Message missing required fields: msg"));

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
        .atMost(Duration.ofSeconds(20))
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
        .atMost(Duration.ofSeconds(20))
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
        .atMost(Duration.ofSeconds(20))
        .until(() -> getUsersByName("joe").size(),
            new IsEqual(2));
  }

  @Test
  public void verifyMultiTableViewForUserCounters() {

    TestUser alice = new TestUser("alice", "alice@foo.com", "Alice Foo");
    TestUser bob = new TestUser("bob", "bob@bar.com", "Bob Bar");

    createUser(alice);
    createUser(bob);

    assignCounter("c1", alice.id);
    assignCounter("c2", bob.id);
    assignCounter("c3", alice.id);
    assignCounter("c4", bob.id);

    increaseCounter("c1", 11);
    increaseCounter("c2", 22);
    increaseCounter("c3", 33);
    increaseCounter("c4", 44);

    // the view is eventually updated

    await()
        .ignoreExceptions()
        .atMost(Duration.ofSeconds(20))
        .until(() -> getUserCounters(alice.id).counters.size(), new IsEqual<>(2));

    await()
        .ignoreExceptions()
        .atMost(Duration.ofSeconds(20))
        .until(() -> getUserCounters(bob.id).counters.size(), new IsEqual<>(2));

    UserCounters aliceCounters = getUserCounters(alice.id);
    Assertions.assertEquals(alice.id, aliceCounters.id);
    Assertions.assertEquals(alice.email, aliceCounters.email);
    Assertions.assertEquals(alice.name, aliceCounters.name);
    Assertions.assertEquals("c1", aliceCounters.counters.get(0).id);
    Assertions.assertEquals(11, aliceCounters.counters.get(0).value);
    Assertions.assertEquals("c3", aliceCounters.counters.get(1).id);
    Assertions.assertEquals(33, aliceCounters.counters.get(1).value);

    UserCounters bobCounters = getUserCounters(bob.id);
    Assertions.assertEquals(bob.id, bobCounters.id);
    Assertions.assertEquals(bob.email, bobCounters.email);
    Assertions.assertEquals(bob.name, bobCounters.name);
    Assertions.assertEquals("c2", bobCounters.counters.get(0).id);
    Assertions.assertEquals(22, bobCounters.counters.get(0).value);
    Assertions.assertEquals("c4", bobCounters.counters.get(1).id);
    Assertions.assertEquals(44, bobCounters.counters.get(1).value);
  }

  @Test
  public void verifyForwardHeaders() throws InterruptedException {

    String actionHeaderValue = "action-value";
    String veHeaderValue = "ve-value";
    String esHeaderValue = "es-value";

    String actionResponse = webClient.get().uri("/forward-headers-action")
        .header(ForwardHeadersAction.SOME_HEADER, actionHeaderValue)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    Assertions.assertEquals(actionHeaderValue, actionResponse);

    String veResponse = webClient.put().uri("/forward-headers-ve/1")
        .header(ForwardHeadersAction.SOME_HEADER, veHeaderValue)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    Assertions.assertEquals(veHeaderValue, veResponse);

    String esResponse = webClient.put().uri("/forward-headers-es/1")
        .header(ForwardHeadersAction.SOME_HEADER, esHeaderValue)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    Assertions.assertEquals(esHeaderValue, esResponse);
  }

  @Test
  public void shouldPropagateMetadataWithHttpDeferredCall() {
    String value = "someValue";

    String actionResponse = webClient.get().uri("/action-with-meta/myKey/" + value)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    Assertions.assertEquals(value, actionResponse);
  }

  @Test
  public void searchWithInstant() {

    var now = Instant.now();
    createCustomer(new CustomerEntity.Customer("customer1", now));

    // the view is eventually updated
    await()
      .ignoreExceptions()
      .atMost(Duration.ofSeconds(20))
      .until(() -> getCustomersByCreationDate(now).size(), new IsEqual(1));

    var later = now.plusSeconds(60 * 5);
    await()
      .ignoreExceptions()
      .atMost(Duration.ofSeconds(20))
      .until(() -> getCustomersByCreationDate(later).size(), new IsEqual(0));
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


  private void createCustomer(CustomerEntity.Customer customer) {
    String created =
      webClient
        .put()
        .uri("/customers/" + customer.name)
        .bodyValue(customer)
        .retrieve()
        .bodyToMono(String.class)
        .block(timeout);
    Assertions.assertEquals("\"Ok\"", created);
  }


  @NotNull
  private List<CustomerEntity.Customer> getCustomersByCreationDate(Instant createdOn) {
    var start = Instant.now();
    var results = webClient
      .post()
      .uri("/customers/by_creation_time")
      .bodyValue(new CustomerByCreationTime.ByTimeRequest(createdOn))
      .retrieve()
      .bodyToMono(CustomerByCreationTime.CustomerList.class)
      .block(timeout)
      .customers();

    var timeElapsed = Duration.between(start, Instant.now());
    logger.info("Searching customer by creation time took: " + timeElapsed.toMillis());
    return results;
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

  private void increaseCounter(String id, int value) {
    webClient
        .post()
        .uri("counter/" + id + "/increase/" + value)
        .retrieve()
        .bodyToMono(Integer.class)
        .block(timeout);
  }

  private void multiplyCounter(String id, int value) {
    webClient
        .post()
        .uri("counter/" + id + "/multiply/" + value)
        .retrieve()
        .bodyToMono(Integer.class)
        .block(timeout);
  }

  private void assignCounter(String id, String assignee) {
    webClient
        .post()
        .uri("assigned-counter/" + id + "/assign/" + assignee)
        .retrieve()
        .bodyToMono(String.class)
        .block(timeout);
  }

  private UserCounters getUserCounters(String userId) {
    return webClient
        .get()
        .uri("user-counters/" + userId)
        .retrieve()
        .bodyToMono(UserCounters.class)
        .block();
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
  public TestUser withEmail(String newEmail) {
    return new TestUser(id, newEmail, name);
  }
}
