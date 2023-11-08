package com.example;

import com.example.actions.CounterCommandFromTopicAction;
import kalix.javasdk.CloudEvent;
// tag::test-topic[]
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
// ...

// end::test-topic[]

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("with-mocked-eventing")
// tag::class[]
@SpringBootTest(classes = Main.class)
@Import(TestKitConfiguration.class)
public class CounterIntegrationTest extends KalixIntegrationTestKitSupport { // <1>

// end::class[]

  private Duration timeout = Duration.of(10, SECONDS);

  @Autowired
  private WebClient webClient;

  // tag::test-topic[]
  @Autowired
  private KalixTestKit kalixTestKit; // <2>
  private EventingTestKit.IncomingMessages commandsTopic;
  private EventingTestKit.OutgoingMessages eventsTopic;
  // end::test-topic[]

  private EventingTestKit.OutgoingMessages eventsTopicWithMeta;

  // tag::test-topic[]

  @BeforeAll
  public void beforeAll() {
    commandsTopic = kalixTestKit.getTopicIncomingMessages("counter-commands"); // <3>
    eventsTopic = kalixTestKit.getTopicOutgoingMessages("counter-events");
    // end::test-topic[]

    eventsTopicWithMeta = kalixTestKit.getTopicOutgoingMessages("counter-events-with-meta");
    // tag::test-topic[]
  }
  // end::test-topic[]

  // since multiple tests are using the same topics, make sure to reset them before each new test
  // so unread messages from previous tests do not mess with the current one
  // tag::clear-topics[]
  @BeforeEach // <1>
  public void clearTopics() {
    eventsTopic.clear(); // <2>
    eventsTopicWithMeta.clear();
  }
  // end::clear-topics[]


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

  // tag::test-topic[]

  @Test
  public void verifyCounterEventSourcedPublishToTopic() throws InterruptedException {
    var counterId = "test-topic";
    var increaseCmd = new CounterCommandFromTopicAction.IncreaseCounter(counterId, 3);
    var multipleCmd = new CounterCommandFromTopicAction.MultiplyCounter(counterId, 4);

    commandsTopic.publish(increaseCmd, counterId); // <4>
    commandsTopic.publish(multipleCmd, counterId);

    var eventIncreased = eventsTopic.expectOneTyped(CounterEvent.ValueIncreased.class); // <5>
    var eventMultiplied = eventsTopic.expectOneTyped(CounterEvent.ValueMultiplied.class);

    assertEquals(increaseCmd.value(), eventIncreased.getPayload().value()); // <6>
    assertEquals(multipleCmd.value(), eventMultiplied.getPayload().value());
  }
  // end::test-topic[]

  // tag::test-topic-metadata[]
  @Test
  public void verifyCounterCommandsAndPublishWithMetadata() {
    var counterId = "test-topic-metadata";
    var increaseCmd = new CounterCommandFromTopicAction.IncreaseCounter(counterId, 10);

    var metadata = CloudEvent.of( // <1>
            "cmd1",
            URI.create("CounterTopicIntegrationTest"),
            increaseCmd.getClass().getName())
        .withSubject(counterId) // <2>
        .asMetadata()
        .add("Content-Type", "application/json"); // <3>

    commandsTopic.publish(kalixTestKit.getMessageBuilder().of(increaseCmd, metadata)); // <4>

    var increasedEvent = eventsTopicWithMeta.expectOneTyped(CounterCommandFromTopicAction.IncreaseCounter.class);
    var actualMd = increasedEvent.getMetadata(); // <5>
    assertEquals(counterId, actualMd.asCloudEvent().subject().get()); // <6>
    assertEquals("application/json", actualMd.get("Content-Type").get());
  }
  // end::test-topic-metadata[]
// tag::class[]
}
// end::class[]
