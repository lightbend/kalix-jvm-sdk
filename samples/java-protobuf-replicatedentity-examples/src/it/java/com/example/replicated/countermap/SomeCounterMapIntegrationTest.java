package com.example.replicated.countermap;

import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import com.example.replicated.Main;
import com.example.replicated.countermap.CounterMapService;
import com.example.replicated.countermap.SomeCounterMapApi;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeCounterMapIntegrationTest {

  /** The test kit starts both the service container and the Kalix Runtime. */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Kalix Runtime. */
  private final CounterMapService client;

  public SomeCounterMapIntegrationTest() {
    client = testKit.getGrpcClient(CounterMapService.class);
  }

  public void increase(String counterMapId, String key, int value) throws Exception {
    client
        .increase(
            SomeCounterMapApi.IncreaseValue.newBuilder()
                .setCounterMapId(counterMapId)
                .setKey(key)
                .setValue(value)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void decrease(String counterMapId, String key, int value) throws Exception {
    client
        .decrease(
            SomeCounterMapApi.DecreaseValue.newBuilder()
                .setCounterMapId(counterMapId)
                .setKey(key)
                .setValue(value)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void remove(String counterMapId, String key) throws Exception {
    client
        .remove(
            SomeCounterMapApi.RemoveValue.newBuilder()
                .setCounterMapId(counterMapId)
                .setKey(key)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public long get(String counterMapId, String key) throws Exception {
    return client
        .get(
            SomeCounterMapApi.GetValue.newBuilder()
                .setCounterMapId(counterMapId)
                .setKey(key)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValue();
  }

  public Map<String, Long> getAll(String counterMapId) throws Exception {
    return client
        .getAll(SomeCounterMapApi.GetAllValues.newBuilder().setCounterMapId(counterMapId).build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValuesMap();
  }

  @Test
  public void interactWithSomeCounterMap() throws Exception {
    assertThat(getAll("countermap1").isEmpty(), is(true));
    increase("countermap1", "key1", 1);
    increase("countermap1", "key1", 2);
    increase("countermap1", "key2", 3);
    decrease("countermap1", "key1", 2);
    decrease("countermap1", "key2", 1);
    increase("countermap1", "key3", 9);
    remove("countermap1", "key3");
    assertThat(get("countermap1", "key1"), is(1L));
    assertThat(get("countermap1", "key2"), is(2L));
    assertThat(get("countermap1", "key3"), is(0L));
    Map<String, Long> allValues = getAll("countermap1");
    assertThat(allValues.size(), is(2));
    assertThat(allValues, allOf(hasEntry("key1", 1L), hasEntry("key2", 2L)));
  }
}
