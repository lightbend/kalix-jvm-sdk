/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;

import com.example.Main;
import com.example.wiring.eventsourcedentities.counter.CounterEvent.ValueIncreased;
import com.example.wiring.valueentities.user.User;
import com.example.wiring.valueentities.user.UserSideEffect;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.EventingTestKit.IncomingMessages;
import kalix.javasdk.testkit.EventingTestKit.Message;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.KalixConfigurationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

import static com.example.wiring.pubsub.PublishESToTopic.COUNTER_EVENTS_TOPIC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("eventing-testkit-subscription")
@SpringBootTest(classes = Main.class)
@Import({KalixConfigurationTest.class, TestkitConfigEventing.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext
public class EventingTestkitIntegrationTest {

  @Autowired
  private KalixTestKit kalixTestKit;
  private IncomingMessages topicSubscription;
  @Autowired
  private WebClient webClient;

  @BeforeAll
  public void beforeAll() {
    topicSubscription = kalixTestKit.getTopicIncomingMessages(COUNTER_EVENTS_TOPIC);
  }

  @BeforeEach
  public void beforeEach() {
    DummyCounterEventStore.clear();
  }


  @AfterAll
  public void afterAll() {
    kalixTestKit.stop();
  }


  @Test
  public void shouldPublishEventWithTypeNameViaSubscriptionEventingTestkit() {
    //given
    String subject = "test-2";
    ValueIncreased event1 = new ValueIncreased(1);
    ValueIncreased event2 = new ValueIncreased(2);

    //when
    Message<ValueIncreased> test = kalixTestKit.getMessageBuilder().of(event1, subject);
    topicSubscription.publish(test);
    topicSubscription.publish(event2, subject);

    //then
    await()
        .ignoreExceptions()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var response = DummyCounterEventStore.get(subject);
          assertThat(response).containsOnly(event1, event2);

          var viewResponse = webClient
              .get()
              .uri("/counter-view-topic-sub/less-then/" + 4)
              .retrieve()
              .bodyToFlux(CounterView.class)
              .toStream()
              .toList();

          assertThat(viewResponse).contains(new CounterView(subject, 3));
        });
  }

  @Test
  public void shouldPublishVEDeleteMessage() {
    //given
    IncomingMessages incomingMessages = kalixTestKit.getValueEntityIncomingMessages("user");
    String subject = "123";
    User user = new User("email", "name");

    //when
    incomingMessages.publish(user, subject);

    //then
    await()
        .ignoreExceptions()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          User consumedUser = UserSideEffect.getUsers().get(subject);
          assertThat(consumedUser).isEqualTo(user);
        });

    //when
    incomingMessages.publishDelete(subject);

    //then
    await()
        .ignoreExceptions()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          User consumedUser = UserSideEffect.getUsers().get(subject);
          assertThat(consumedUser).isNull();
        });
  }
}
