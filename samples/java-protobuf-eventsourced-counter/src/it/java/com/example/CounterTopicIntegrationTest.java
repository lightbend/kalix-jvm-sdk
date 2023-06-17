/* This code was generated by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package com.example;

import com.example.actions.CounterTopicApi;
import kalix.javasdk.CloudEvent;
// tag::test-topic[]
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
// ...
// end::test-topic[]
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
// tag::test-topic[]

public class CounterTopicIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix proxy.
   */
  @ClassRule
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(Main.createKalix()); // <1>

  private static EventingTestKit.Topic commandsTopic;
  private static EventingTestKit.Topic eventsTopic;
  // end::test-topic[]

  private static EventingTestKit.Topic eventsTopicWithMeta;

 // tag::test-topic[]

  public CounterTopicIntegrationTest() {
    commandsTopic = testKit.getTopic("counter-commands"); // <2>
    eventsTopic = testKit.getTopic("counter-events"); // <3>
    // end::test-topic[]
    eventsTopicWithMeta = testKit.getTopic("counter-events-with-meta");
  // tag::test-topic[]
  }
  // end::test-topic[]


  // since multiple tests are using the same topics, make sure to reset them before each new test
  // so unread messages from previous tests do not mess with the current one
  // tag::clear-topics[]
  @Before // <1>
  public void clearTopics() {
    commandsTopic.clear(); // <2>
    eventsTopic.clear();
    eventsTopicWithMeta.clear();
  }
  // end::clear-topics[]
  // tag::test-topic[]

  @Test
  public void verifyCounterCommandsAndPublish() {
    var counterId = "test-topic";

    var increaseCmd = CounterApi.IncreaseValue.newBuilder().setCounterId(counterId).setValue(4).build();
    var decreaseCmd = CounterApi.DecreaseValue.newBuilder().setCounterId(counterId).setValue(1).build();
    commandsTopic.publish(increaseCmd, counterId); // <4>
    commandsTopic.publish(decreaseCmd, counterId);

    var increasedEvent = eventsTopic.expectOneTyped(CounterTopicApi.Increased.class); // <5>
    var decreasedEvent = eventsTopic.expectOneTyped(CounterTopicApi.Decreased.class);
    assertEquals(increaseCmd.getValue(), increasedEvent.getPayload().getValue()); // <6>
    assertEquals(decreaseCmd.getValue(), decreasedEvent.getPayload().getValue());
  }
  // end::test-topic[]

  // tag::test-topic-metadata[]
  @Test
  public void verifyCounterCommandsAndPublishWithMetadata() {
    var counterId = "test-topic-metadata";
    var increaseCmd = CounterApi.IncreaseValue.newBuilder().setCounterId(counterId).setValue(4).build();

    var md = CloudEvent.of( // <1>
            "cmd1",
            URI.create("CounterTopicIntegrationTest"),
            increaseCmd.getDescriptorForType().getFullName())
        .withSubject(counterId) // <2>
        .asMetadata()
        .add("Content-Type", "application/protobuf"); // <3>

    commandsTopic.publish(EventingTestKit.Message.of(increaseCmd, md)); // <4>

    var increasedEvent = eventsTopicWithMeta.expectOneTyped(CounterTopicApi.Increased.class);
    var expectedMd = increasedEvent.getMetadata(); // <5>
    assertEquals(counterId, expectedMd.asCloudEvent().subject().get()); // <6>
    assertEquals("application/protobuf", expectedMd.get("Content-Type").get());
  }
  // end::test-topic-metadata[]
  // tag::test-topic[]
}
// end::test-topic[]
