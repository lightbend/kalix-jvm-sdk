/* This code was generated by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package com.example;

import kalix.javasdk.testkit.junit.KalixTestKitResource;
import com.example.CounterApi;
import com.example.CounterService;
import com.example.Main;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

// Example of an integration test calling our service via the Akka Serverless proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CounterIntegrationTest {

  /**
   * The test kit starts both the service container and the Akka Serverless proxy.
   */
  @ClassRule
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Akka Serverless proxy.
   */
  private final CounterService client;

  public CounterIntegrationTest() {
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
  public void increaseOnNonExistingEntity() throws Exception {
    String counterId = "test-1";
    awaitResult(client.increase(newIncreaseMessage(counterId, 1)));
    assertEquals(1, getCounterValue(counterId));
  }


  @Test
  public void decreaseOnNonExistingEntity() throws Exception {
    String counterId = "test-2";

    awaitResult(client.increase(newIncreaseMessage(counterId, 5)));
    assertEquals(5, getCounterValue(counterId));

    awaitResult(
        client.decrease(
            CounterApi.DecreaseValue.newBuilder()
                .setCounterId(counterId)
                .setValue(-1).build()));
    assertEquals(4, getCounterValue(counterId));
  }


  @Test
  public void resetOnNonExistingEntity() throws Exception {
    String counterId = "test-3";
    awaitResult(client.increase(newIncreaseMessage(counterId, 5)));
    assertEquals(5, getCounterValue(counterId));

    awaitResult(
        client.reset(CounterApi.ResetValue.newBuilder().setCounterId(counterId).build()));
    assertEquals(0, getCounterValue(counterId));
  }

}
