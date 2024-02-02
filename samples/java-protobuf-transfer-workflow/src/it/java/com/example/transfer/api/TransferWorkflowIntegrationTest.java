package com.example.transfer.api;

import com.example.Main;
import com.example.transfer.api.TransferApi.Transfer;
import com.example.wallet.api.WalletApi.GetRequest;
import com.example.wallet.api.WalletApi.InitialBalance;
import com.example.wallet.api.WalletService;
import com.example.wallet.domain.WalletDomain.WalletState;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class TransferWorkflowIntegrationTest {

  @RegisterExtension
  public static final KalixTestKitExtension testKit =
    new KalixTestKitExtension(Main.createKalix());

  private final TransferWorkflowService workflowClient;
  private final WalletService walletClient;

  public TransferWorkflowIntegrationTest() {
    workflowClient = testKit.getGrpcClient(TransferWorkflowService.class);
    walletClient = testKit.getGrpcClient(WalletService.class);
  }

  @Test
  public void shouldTransferFunds() throws Exception {
    InitialBalance initWalletA = InitialBalance.newBuilder().setWalletId("a").setBalance(100).build();
    InitialBalance initWalletB = InitialBalance.newBuilder().setWalletId("b").setBalance(100).build();
    walletClient.create(initWalletA).toCompletableFuture().get(5, SECONDS);
    walletClient.create(initWalletB).toCompletableFuture().get(5, SECONDS);

    Transfer transfer = Transfer.newBuilder().setTransferId("1").setFrom("a").setTo("b").setAmount(10).build();
    workflowClient.start(transfer).toCompletableFuture().get(5, SECONDS);

    await().atMost(10, SECONDS).untilAsserted(() -> {
      WalletState walletA = walletClient.getWalletState(GetRequest.newBuilder().setWalletId("a").build())
        .toCompletableFuture().get(5, SECONDS);
      WalletState walletB = walletClient.getWalletState(GetRequest.newBuilder().setWalletId("b").build())
        .toCompletableFuture().get(5, SECONDS);

      assertEquals(90, walletA.getBalance());
      assertEquals(110, walletB.getBalance());
    });
  }
}
