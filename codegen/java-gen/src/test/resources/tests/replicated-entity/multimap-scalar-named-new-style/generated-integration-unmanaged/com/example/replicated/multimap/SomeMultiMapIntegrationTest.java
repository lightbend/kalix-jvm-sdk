package com.example.replicated.multimap;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.junit.AkkaServerlessTestKitResource;
import org.example.Main;
import org.junit.ClassRule;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Akka Serverless proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeMultiMapIntegrationTest {

  /**
   * The test kit starts both the service container and the Akka Serverless proxy.
   */
  @ClassRule
  public static final AkkaServerlessTestKitResource testKit =
    new AkkaServerlessTestKitResource(Main.createAkkaServerless());

  /**
   * Use the generated gRPC client to call the service through the Akka Serverless proxy.
   */
  private final MultiMapService client;

  public SomeMultiMapIntegrationTest() {
    client = testKit.getGrpcClient(MultiMapService.class);
  }

  @Test
  public void putOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.put(SomeMultiMapApi.PutValue.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }
}
