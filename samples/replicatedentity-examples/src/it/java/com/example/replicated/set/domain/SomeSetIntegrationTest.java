package com.example.replicated.set.domain;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import com.example.replicated.Main;
import com.example.replicated.set.SetServiceClient;
import com.example.replicated.set.SomeSetApi;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeSetIntegrationTest {

  /** The test kit starts both the service container and the Akka Serverless proxy. */
  @ClassRule
  public static final AkkaServerlessTestkitResource testkit =
      new AkkaServerlessTestkitResource(Main.createAkkaServerless());

  /** Use the generated gRPC client to call the service through the Akka Serverless proxy. */
  private final SetServiceClient client;

  public SomeSetIntegrationTest() {
    client = SetServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
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
