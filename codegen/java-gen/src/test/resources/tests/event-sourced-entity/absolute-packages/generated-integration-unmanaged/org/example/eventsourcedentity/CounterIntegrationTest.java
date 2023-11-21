package org.example.eventsourcedentity;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import org.example.Main;
import org.example.state.OuterCounterState;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.concurrent.TimeUnit.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CounterIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix proxy.
   */
  @RegisterExtension
  public static final KalixTestKitResource testKit =
    new KalixTestKitResource(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix proxy.
   */
  private final CounterService client;

  public CounterIntegrationTest() {
    client = testKit.getGrpcClient(CounterService.class);
  }

  @Test
  @Disabled("to be implemented")
  public void increaseOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.increase(CounterApi.IncreaseValue.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  @Disabled("to be implemented")
  public void decreaseOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.decrease(CounterApi.DecreaseValue.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }
}
