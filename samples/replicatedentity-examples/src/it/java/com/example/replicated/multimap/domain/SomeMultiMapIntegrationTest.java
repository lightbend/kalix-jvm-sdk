package com.example.replicated.multimap.domain;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import com.example.replicated.Main;
import com.example.replicated.multimap.MultiMapServiceClient;
import com.example.replicated.multimap.SomeMultiMapApi;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeMultiMapIntegrationTest {

  /** The test kit starts both the service container and the Akka Serverless proxy. */
  @ClassRule
  public static final AkkaServerlessTestkitResource testkit =
      new AkkaServerlessTestkitResource(Main.createAkkaServerless());

  /** Use the generated gRPC client to call the service through the Akka Serverless proxy. */
  private final MultiMapServiceClient client;

  public SomeMultiMapIntegrationTest() {
    client =
        MultiMapServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
  }

  public void put(String multiMapId, String key, String value) throws Exception {
    client
        .put(
            SomeMultiMapApi.PutValue.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(SomeMultiMapApi.Key.newBuilder().setKey(key))
                .setValue(SomeMultiMapApi.Value.newBuilder().setValue(value))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void putAll(String multiMapId, String key, String... values) throws Exception {
    client
        .putAll(
            SomeMultiMapApi.PutAllValues.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(SomeMultiMapApi.Key.newBuilder().setKey(key))
                .addAllValues(
                    Stream.of(values)
                        .map(value -> SomeMultiMapApi.Value.newBuilder().setValue(value).build())
                        .collect(Collectors.toList()))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void remove(String multiMapId, String key, String value) throws Exception {
    client
        .remove(
            SomeMultiMapApi.RemoveValue.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(SomeMultiMapApi.Key.newBuilder().setKey(key))
                .setValue(SomeMultiMapApi.Value.newBuilder().setValue(value))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void removeAll(String multiMapId, String key) throws Exception {
    client
        .removeAll(
            SomeMultiMapApi.RemoveAllValues.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(SomeMultiMapApi.Key.newBuilder().setKey(key))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public List<String> get(String multiMapId, String key) throws Exception {
    return client
        .get(
            SomeMultiMapApi.GetValues.newBuilder()
                .setMultiMapId(multiMapId)
                .setKey(SomeMultiMapApi.Key.newBuilder().setKey(key))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValuesList()
        .stream()
        .map(SomeMultiMapApi.Value::getValue)
        .collect(Collectors.toList());
  }

  public Map<String, List<String>> getAll(String multiMapId) throws Exception {
    return client
        .getAll(SomeMultiMapApi.GetAllValues.newBuilder().setMultiMapId(multiMapId).build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValuesList()
        .stream()
        .collect(
            Collectors.toMap(
                key -> key.getKey().getKey(),
                value ->
                    value.getValuesList().stream()
                        .map(SomeMultiMapApi.Value::getValue)
                        .collect(Collectors.toList())));
  }

  @Test
  public void interactWithSomeMultiMap() throws Exception {
    assertThat(getAll("multimap1").isEmpty(), is(true));
    put("multimap1", "1", "A");
    put("multimap1", "1", "B");
    put("multimap1", "1", "C");
    put("multimap1", "1", "D");
    putAll("multimap1", "1", "D", "E", "F");
    put("multimap1", "2", "X");
    put("multimap1", "2", "Y");
    putAll("multimap1", "2", "X", "Z");
    putAll("multimap1", "3", "P", "Q", "R", "S");
    remove("multimap1", "1", "B");
    remove("multimap1", "1", "D");
    remove("multimap1", "1", "F");
    removeAll("multimap1", "3");
    assertThat(get("multimap1", "1"), contains("A", "C", "E"));
    assertThat(get("multimap1", "2"), contains("X", "Y", "Z"));
    assertThat(get("multimap1", "3").isEmpty(), is(true));
    Map<String, List<String>> allValues = getAll("multimap1");
    assertThat(allValues.size(), is(2));
    assertThat(
        allValues,
        allOf(
            hasEntry(is("1"), contains("A", "C", "E")),
            hasEntry(is("2"), contains("X", "Y", "Z"))));
  }
}
