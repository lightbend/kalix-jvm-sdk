package com.example.replicated.counter;

import kalix.javasdk.testkit.junit.KalixTestKitResource;
import com.example.replicated.Main;
import com.example.replicated.counter.CounterService;
import com.example.replicated.counter.SomeCounterApi;
import org.junit.ClassRule;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.assertEquals;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeCounterIntegrationTest {

  /** The test kit starts both the service container and the Kalix proxy. */
  @ClassRule
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Kalix proxy. */
  private final CounterService client;

  public SomeCounterIntegrationTest() {
    client = testKit.getGrpcClient(CounterService.class);
  }

  public void increase(String counterId, int value) throws Exception {
    client
        .increase(
            SomeCounterApi.IncreaseValue.newBuilder()
                .setCounterId(counterId)
                .setValue(value)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void decrease(String counterId, int value) throws Exception {
    client
        .decrease(
            SomeCounterApi.DecreaseValue.newBuilder()
                .setCounterId(counterId)
                .setValue(value)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public SomeCounterApi.CurrentValue get(String counterId) throws Exception {
    return client
        .get(SomeCounterApi.GetValue.newBuilder().setCounterId(counterId).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  @Test
  public void interactWithSomeCounter() throws Exception {
    assertEquals(0, get("counter1").getValue());
    increase("counter1", 1);
    increase("counter1", 2);
    increase("counter1", 3);
    decrease("counter1", 4);
    assertEquals(2, get("counter1").getValue());
  }
}
