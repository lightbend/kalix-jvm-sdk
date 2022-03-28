package com.example.replicated.register;

import kalix.javasdk.testkit.junit.AkkaServerlessTestKitResource;
import com.example.replicated.Main;
import com.example.replicated.register.RegisterService;
import com.example.replicated.register.SomeRegisterApi;
import org.junit.ClassRule;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.assertEquals;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Akka Serverless proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class SomeRegisterIntegrationTest {

  /** The test kit starts both the service container and the Akka Serverless proxy. */
  @ClassRule
  public static final AkkaServerlessTestKitResource testKit =
      new AkkaServerlessTestKitResource(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Akka Serverless proxy. */
  private final RegisterService client;

  public SomeRegisterIntegrationTest() {
    client = testKit.getGrpcClient(RegisterService.class);
  }

  public void set(String registerId, String value) throws Exception {
    client
        .set(
            SomeRegisterApi.SetValue.newBuilder().setRegisterId(registerId).setValue(value).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  public SomeRegisterApi.CurrentValue get(String registerId) throws Exception {
    return client
        .get(SomeRegisterApi.GetValue.newBuilder().setRegisterId(registerId).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  @Test
  public void interactWithSomeRegister() throws Exception {
    assertEquals("", get("register1").getValue());
    set("register1", "one");
    assertEquals("one", get("register1").getValue());
    set("register1", "two");
    assertEquals("two", get("register1").getValue());
  }
}
