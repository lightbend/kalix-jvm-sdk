package com.example.replicated.multimap;

import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import com.example.replicated.Main;
import com.example.replicated.multimap.MultiMapService;
import com.example.replicated.multimap.SomeMultiMapApi;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeMultiMapIntegrationTest {

  /** The test kit starts both the service container and the Kalix Runtime. */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Kalix Runtime. */
  private final MultiMapService client;

  public SomeMultiMapIntegrationTest() {
    client = testKit.getGrpcClient(MultiMapService.class);
  }

  public void put(String multiMapId, String key, double value) throws Exception {
    client
        .put(
            SomeMultiMapApi.PutValue.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(key)
                .setValue(value)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void putAll(String multiMapId, String key, Double... values) throws Exception {
    client
        .putAll(
            SomeMultiMapApi.PutAllValues.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(key)
                .addAllValues(Arrays.asList(values))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void remove(String multiMapId, String key, double value) throws Exception {
    client
        .remove(
            SomeMultiMapApi.RemoveValue.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(key)
                .setValue(value)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void removeAll(String multiMapId, String key) throws Exception {
    client
        .removeAll(
            SomeMultiMapApi.RemoveAllValues.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(key)
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public List<Double> get(String multiMapId, String key) throws Exception {
    return client
        .get(SomeMultiMapApi.GetValues.newBuilder().setMultiMapId(multiMapId).setKey(key).build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValuesList();
  }

  public Map<String, List<Double>> getAll(String multiMapId) throws Exception {
    return client
        .getAll(SomeMultiMapApi.GetAllValues.newBuilder().setMultiMapId(multiMapId).build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValuesList()
        .stream()
        .collect(
            Collectors.toMap(
                SomeMultiMapApi.CurrentValues::getKey,
                SomeMultiMapApi.CurrentValues::getValuesList));
  }

  @Test
  public void interactWithSomeMultiMap() throws Exception {
    assertThat(getAll("multimap1").isEmpty(), is(true));
    put("multimap1", "A", 0.1);
    put("multimap1", "A", 0.2);
    put("multimap1", "A", 0.3);
    put("multimap1", "A", 0.4);
    putAll("multimap1", "A", 0.1, 0.4, 0.5, 0.6);
    put("multimap1", "B", 3.14159);
    put("multimap1", "B", 2.71828);
    putAll("multimap1", "B", 3.14159, 1.61803);
    putAll("multimap1", "C", 1.2, 3.4, 5.6, 7.8);
    remove("multimap1", "A", 0.2);
    remove("multimap1", "A", 0.4);
    remove("multimap1", "A", 0.6);
    removeAll("multimap1", "C");
    assertThat(get("multimap1", "A"), contains(0.1, 0.3, 0.5));
    assertThat(get("multimap1", "B"), contains(1.61803, 2.71828, 3.14159));
    assertThat(get("multimap1", "C").isEmpty(), is(true));
    Map<String, List<Double>> allValues = getAll("multimap1");
    assertThat(allValues.size(), is(2));
    assertThat(
        allValues,
        allOf(
            hasEntry(is("A"), contains(0.1, 0.3, 0.5)),
            hasEntry(is("B"), contains(1.61803, 2.71828, 3.14159))));
  }
}
