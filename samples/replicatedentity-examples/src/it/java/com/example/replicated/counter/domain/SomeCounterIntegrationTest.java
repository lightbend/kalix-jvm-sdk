package com.example.replicated.counter.domain;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import com.example.replicated.Main;
import com.example.replicated.counter.CounterServiceClient;
import com.example.replicated.counter.SomeCounterApi;
import org.junit.ClassRule;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.assertEquals;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeCounterIntegrationTest {

  /** The test kit starts both the service container and the Akka Serverless proxy. */
  @ClassRule
  public static final AkkaServerlessTestkitResource testkit =
      new AkkaServerlessTestkitResource(Main.createAkkaServerless());

  /** Use the generated gRPC client to call the service through the Akka Serverless proxy. */
  private final CounterServiceClient client;

  public SomeCounterIntegrationTest() {
    client = CounterServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
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
