package com.example.wallet.api;

import com.example.Main;
import com.example.wallet.domain.WalletDomain;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.concurrent.TimeUnit.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class WalletIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix Runtime.
   */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
    new KalixTestKitExtension(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix Runtime.
   */
  private final WalletService client;

  public WalletIntegrationTest() {
    client = testKit.getGrpcClient(WalletService.class);
  }

  @Test
  @Disabled("to be implemented")
  public void testCreate() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.create(WalletApi.InitialBalance.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  @Disabled("to be implemented")
  public void testWithdraw() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.withdraw(WalletApi.WithdrawRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  @Disabled("to be implemented")
  public void testDeposit() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.deposit(WalletApi.DepositRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  @Disabled("to be implemented")
  public void testGetWalletState() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.getWalletState(WalletApi.GetRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }
}
