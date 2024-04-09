/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;

import com.example.Main;
import com.example.wiring.valueentities.customer.CustomerEntity;
import com.example.wiring.valueentities.customer.CustomerEntity.Customer;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.KalixConfigurationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.wiring.pubsub.PublishVEToTopic.CUSTOMERS_TOPIC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("eventing-testkit-destination")
@SpringBootTest(classes = Main.class)
@Import({KalixConfigurationTest.class, TestkitConfigEventing.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext
public class EventingTestkitDestinationIntegrationTest {

  @Autowired
  private KalixTestKit kalixTestKit;
  private EventingTestKit.OutgoingMessages destination;
  @Autowired
  private ComponentClient componentClient;

  @BeforeAll
  public void beforeAll() {
    destination = kalixTestKit.getTopicOutgoingMessages(CUSTOMERS_TOPIC);
  }

  @AfterAll
  public void afterAll() {
    kalixTestKit.stop();
  }

  @Test
  public void shouldPublishEventWithTypeNameViaEventingTestkit() throws ExecutionException, InterruptedException, TimeoutException {
    //given
    String subject = "test";
    Customer customer = new Customer("andre", Instant.now());

    //when
    componentClient.forValueEntity(subject).call(CustomerEntity::create).params(customer)
        .execute().toCompletableFuture().get(5, TimeUnit.SECONDS);

    //then
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          Customer publishedCustomer = destination.expectOneTyped(Customer.class).getPayload();
          assertThat(publishedCustomer).isEqualTo(customer);
        });
  }
}
