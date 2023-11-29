package com.example.replicated.map;

import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import com.example.replicated.Main;
import com.example.replicated.map.MapService;
import com.example.replicated.map.SomeMapApi;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeMapIntegrationTest {

  /** The test kit starts both the service container and the Kalix Runtime. */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Kalix Runtime. */
  private final MapService client;

  public SomeMapIntegrationTest() {
    client = testKit.getGrpcClient(MapService.class);
  }

  public void increaseFoo(String mapId, int value) throws Exception {
    client
        .increaseFoo(
            SomeMapApi.IncreaseFooValue.newBuilder().setMapId(mapId).setValue(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void decreaseFoo(String mapId, int value) throws Exception {
    client
        .decreaseFoo(
            SomeMapApi.DecreaseFooValue.newBuilder().setMapId(mapId).setValue(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void setBar(String mapId, String value) throws Exception {
    client
        .setBar(SomeMapApi.SetBarValue.newBuilder().setMapId(mapId).setValue(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void addBaz(String mapId, String value) throws Exception {
    client
        .addBaz(SomeMapApi.AddBazValue.newBuilder().setMapId(mapId).setValue(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void removeBaz(String mapId, String value) throws Exception {
    client
        .removeBaz(SomeMapApi.RemoveBazValue.newBuilder().setMapId(mapId).setValue(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public SomeMapApi.CurrentValues get(String mapId) throws Exception {
    return client
        .get(SomeMapApi.GetValues.newBuilder().setMapId(mapId).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  @Test
  public void interactWithSomeMap() throws Exception {
    SomeMapApi.CurrentValues emptyValues = get("map1");
    assertThat(emptyValues.getFoo(), is(0L));
    assertThat(emptyValues.getBar(), is(""));
    assertThat(emptyValues.getBazList().isEmpty(), is(true));
    increaseFoo("map1", 1);
    increaseFoo("map1", 2);
    increaseFoo("map1", 3);
    decreaseFoo("map1", 4);
    setBar("map1", "one");
    setBar("map1", "two");
    addBaz("map1", "A");
    addBaz("map1", "B");
    addBaz("map1", "B");
    addBaz("map1", "A");
    addBaz("map1", "X");
    addBaz("map1", "Y");
    addBaz("map1", "Z");
    removeBaz("map1", "B");
    removeBaz("map1", "Y");
    SomeMapApi.CurrentValues currentValues = get("map1");
    assertThat(currentValues.getFoo(), is(2L));
    assertThat(currentValues.getBar(), is("two"));
    assertThat(currentValues.getBazList(), contains("A", "X", "Z"));
  }
}
