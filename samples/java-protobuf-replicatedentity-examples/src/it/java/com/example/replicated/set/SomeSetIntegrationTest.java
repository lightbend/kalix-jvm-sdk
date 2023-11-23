package com.example.replicated.set;

import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import com.example.replicated.Main;
import com.example.replicated.set.SetService;
import com.example.replicated.set.SomeSetApi;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.List;

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
public class SomeSetIntegrationTest {

  /** The test kit starts both the service container and the Kalix Runtime. */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Kalix Runtime. */
  private final SetService client;

  public SomeSetIntegrationTest() {
    client = testKit.getGrpcClient(SetService.class);
  }

  public void add(String setId, String value) throws Exception {
    client
        .add(SomeSetApi.AddElement.newBuilder().setSetId(setId).setElement(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public void remove(String setId, String value) throws Exception {
    client
        .remove(SomeSetApi.RemoveElement.newBuilder().setSetId(setId).setElement(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public List<String> get(String setId) throws Exception {
    return client
        .get(SomeSetApi.GetElements.newBuilder().setSetId(setId).build())
        .toCompletableFuture()
        .get(5, SECONDS)
        .getElementsList();
  }

  @Test
  public void interactWithSomeSet() throws Exception {
    assertThat(get("set1").isEmpty(), is(true));
    add("set1", "one");
    add("set1", "foo");
    add("set1", "two");
    add("set1", "one");
    remove("set1", "foo");
    assertThat(get("set1"), contains("one", "two"));
  }
}
