package com.example.replicated.registermap;

import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import com.example.replicated.Main;
import com.example.replicated.registermap.RegisterMapService;
import com.example.replicated.registermap.SomeRegisterMapApi;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

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
public class SomeRegisterMapIntegrationTest {

  /** The test kit starts both the service container and the Kalix Runtime. */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Kalix Runtime. */
  private final RegisterMapService client;

  public SomeRegisterMapIntegrationTest() {
    client = testKit.getGrpcClient(RegisterMapService.class);
  }

  public void set(String registerMapId, String key, String value) throws Exception {
    client
        .set(
            SomeRegisterMapApi.SetValue.newBuilder()
                .setRegisterMapId(registerMapId)
                .setKey(SomeRegisterMapApi.Key.newBuilder().setField(key))
                .setValue(SomeRegisterMapApi.Value.newBuilder().setField(value))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void remove(String registerMapId, String key) throws Exception {
    client
        .remove(
            SomeRegisterMapApi.RemoveValue.newBuilder()
                .setRegisterMapId(registerMapId)
                .setKey(SomeRegisterMapApi.Key.newBuilder().setField(key))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public String get(String registerMapId, String key) throws Exception {
    return client
        .get(
            SomeRegisterMapApi.GetValue.newBuilder()
                .setRegisterMapId(registerMapId)
                .setKey(SomeRegisterMapApi.Key.newBuilder().setField(key))
                .build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValue()
        .getField();
  }

  public Map<String, String> getAll(String registerMapId) throws Exception {
    return client
        .getAll(
            SomeRegisterMapApi.GetAllValues.newBuilder().setRegisterMapId(registerMapId).build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getValuesList()
        .stream()
        .collect(
            Collectors.toMap(key -> key.getKey().getField(), value -> value.getValue().getField()));
  }

  @Test
  public void interactWithSomeRegisterMap() throws Exception {
    assertThat(getAll("registermap1").isEmpty(), is(true));
    set("registermap1", "key1", "value1");
    set("registermap1", "key2", "value2");
    set("registermap1", "key3", "value3");
    set("registermap1", "key1", "one");
    set("registermap1", "key2", "two");
    remove("registermap1", "key3");
    assertThat(get("registermap1", "key1"), is("one"));
    assertThat(get("registermap1", "key2"), is("two"));
    assertThat(get("registermap1", "key3"), is(""));
    Map<String, String> allValues = getAll("registermap1");
    assertThat(allValues.size(), is(2));
    assertThat(allValues, allOf(hasEntry("key1", "one"), hasEntry("key2", "two")));
  }
}
