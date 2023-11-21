package com.example.replicated.multimap;

import com.example.replicated.multimap.domain.SomeMultiMapDomain;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import org.example.Main;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeMultiMapIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix proxy.
   */
  @RegisterExtension
  public static final KalixTestKitResource testKit =
    new KalixTestKitResource(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix proxy.
   */
  private final MultiMapService client;

  public SomeMultiMapIntegrationTest() {
    client = testKit.getGrpcClient(MultiMapService.class);
  }

  @Test
  @Disabled("to be implemented")
  public void putOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.put(SomeMultiMapApi.PutValue.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }
}
