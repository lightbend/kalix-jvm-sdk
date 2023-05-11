/* This code was generated by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package com.example;

import com.example.CounterApi;
import com.example.CounterService;
import com.example.Main;
import com.example.actions.CounterTopicApi;
import com.google.protobuf.ByteString;
import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import kalix.testkit.protocol.eventing_test_backend.Message;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CounterTopicSubscriptionIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix proxy.
   */
  @ClassRule
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix proxy.
   */
  private final CounterService client;

  public CounterTopicSubscriptionIntegrationTest() {
    client = testKit.getGrpcClient(CounterService.class);
  }

  private CounterApi.IncreaseValue newIncreaseMessage(String id, int value) {
    return CounterApi.IncreaseValue.newBuilder()
        .setCounterId(id)
        .setValue(value)
        .build();
  }

  private int getCounterValue(String counterId) throws Exception {
    CounterApi.GetCounter getCounter = CounterApi.GetCounter.newBuilder().setCounterId(counterId).build();
    return awaitResult(client.getCurrentCounter(getCounter)).getValue();
  }

  private <T> T awaitResult(CompletionStage<T> stage) throws Exception {
    return stage.toCompletableFuture().get(5, SECONDS);
  }

  @Test
  public void increaseOnReadingFromTopic() throws Exception {

    var topic = testKit.getTopic("counter-events");
    var msg = CounterTopicApi.Increased.newBuilder().setValue(15).build();
    var md = Metadata.EMPTY
        .add("Content-Type", "application/protobuf;proto="+ CounterTopicApi.Increased.getDescriptor().getFullName())
        .add("ce-specversion", "1.0")
        .add("ce-id", "msg1")
        .add("ce-type", "Increase")
        .add("ce-source", CounterTopicApi.Increased.getDescriptor().getFullName())
        .add("ce-subject", "counter-2");

    topic.publish(msg.toByteString(), md);

    assertEquals(15, getCounterValue("counter-2"));
  }


}
