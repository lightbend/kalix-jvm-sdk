package com.example.transfer.api;

import com.example.Main;
import com.example.transfer.api.TransferApi.AcceptRequest;
import com.example.transfer.api.TransferApi.AcceptationTimeoutRequest;
import com.example.transfer.api.TransferApi.Transfer;
import com.example.transfer.domain.TransferDomain.TransferState;
import com.example.wallet.api.WalletApi.GetRequest;
import com.example.wallet.api.WalletApi.InitialBalance;
import com.example.wallet.api.WalletService;
import com.example.wallet.domain.WalletDomain.WalletState;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.example.transfer.domain.TransferDomain.TransferStatus.COMPENSATION_COMPLETED;
import static com.example.transfer.domain.TransferDomain.TransferStatus.REQUIRES_MANUAL_INTERVENTION;
import static com.example.transfer.domain.TransferDomain.TransferStatus.TRANSFER_ACCEPTATION_TIMED_OUT;
import static com.example.transfer.domain.TransferDomain.TransferStatus.WAITING_FOR_ACCEPTATION;
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
  public void showTransferFunds() throws Exception {
    String walletAId = randomId();
    String walletBId = randomId();
    InitialBalance initWalletA = InitialBalance.newBuilder().setWalletId(walletAId).setBalance(100).build();
    InitialBalance initWalletB = InitialBalance.newBuilder().setWalletId(walletBId).setBalance(100).build();
    walletClient.create(initWalletA).toCompletableFuture().get(5, SECONDS);
    walletClient.create(initWalletB).toCompletableFuture().get(5, SECONDS);

    Transfer transfer = Transfer.newBuilder().setTransferId("1").setFrom(walletAId).setTo(walletBId).setAmount(10).build();
    workflowClient.start(transfer).toCompletableFuture().get(5, SECONDS);

    await().atMost(10, SECONDS).untilAsserted(() -> {
      WalletState walletA = walletClient.getWalletState(GetRequest.newBuilder().setWalletId(walletAId).build())
        .toCompletableFuture().get(5, SECONDS);
      WalletState walletB = walletClient.getWalletState(GetRequest.newBuilder().setWalletId(walletBId).build())
        .toCompletableFuture().get(5, SECONDS);

      assertEquals(90, walletA.getBalance());
      assertEquals(110, walletB.getBalance());
    });
  }

  @Test
  public void shouldTransferFundsWithAcceptation() throws Exception {
    String walletAId = randomId();
    String walletBId = randomId();
    String transferId = randomId();
    InitialBalance initWalletA = InitialBalance.newBuilder().setWalletId(walletAId).setBalance(2000).build();
    InitialBalance initWalletB = InitialBalance.newBuilder().setWalletId(walletBId).setBalance(100).build();
    walletClient.create(initWalletA).toCompletableFuture().get(5, SECONDS);
    walletClient.create(initWalletB).toCompletableFuture().get(5, SECONDS);

    Transfer transfer = Transfer.newBuilder().setTransferId(transferId).setFrom(walletAId).setTo(walletBId).setAmount(1001).build();
    workflowClient.start(transfer).toCompletableFuture().get(5, SECONDS);

    TransferState transferState = workflowClient.getTransferState(getTransferRequest(transferId)).toCompletableFuture().get(5, SECONDS);
    assertEquals(WAITING_FOR_ACCEPTATION, transferState.getStatus());

    workflowClient.accept(AcceptRequest.newBuilder().setTransferId(transferId).build()).toCompletableFuture().get(5, SECONDS);


    await().atMost(10, SECONDS).untilAsserted(() -> {
      WalletState walletA = walletClient.getWalletState(GetRequest.newBuilder().setWalletId(walletAId).build())
        .toCompletableFuture().get(5, SECONDS);
      WalletState walletB = walletClient.getWalletState(GetRequest.newBuilder().setWalletId(walletBId).build())
        .toCompletableFuture().get(5, SECONDS);

      assertEquals(999, walletA.getBalance());
      assertEquals(1101, walletB.getBalance());
    });
  }

  @Test
  public void shouldTimeoutTransferAcceptation() throws Exception {
    String walletAId = randomId();
    String walletBId = randomId();
    String transferId = randomId();
    InitialBalance initWalletA = InitialBalance.newBuilder().setWalletId(walletAId).setBalance(2000).build();
    InitialBalance initWalletB = InitialBalance.newBuilder().setWalletId(walletBId).setBalance(100).build();
    walletClient.create(initWalletA).toCompletableFuture().get(5, SECONDS);
    walletClient.create(initWalletB).toCompletableFuture().get(5, SECONDS);

    Transfer transfer = Transfer.newBuilder().setTransferId(transferId).setFrom(walletAId).setTo(walletBId).setAmount(1001).build();
    workflowClient.start(transfer).toCompletableFuture().get(5, SECONDS);

    workflowClient.acceptationTimeout(AcceptationTimeoutRequest.newBuilder().setTransferId(transferId).build()).toCompletableFuture().get(5, SECONDS);

    TransferState transferState = workflowClient.getTransferState(getTransferRequest(transferId)).toCompletableFuture().get(5, SECONDS);
    assertEquals(TRANSFER_ACCEPTATION_TIMED_OUT, transferState.getStatus());

    WalletState walletA = walletClient.getWalletState(GetRequest.newBuilder().setWalletId(walletAId).build())
      .toCompletableFuture().get(5, SECONDS);
    WalletState walletB = walletClient.getWalletState(GetRequest.newBuilder().setWalletId(walletBId).build())
      .toCompletableFuture().get(5, SECONDS);

    assertEquals(2000, walletA.getBalance());
    assertEquals(100, walletB.getBalance());
  }

  @Test
  public void shouldCompensateFailedTransferToNotExistingWallet() throws ExecutionException, InterruptedException, TimeoutException {
    String walletAId = randomId();
    String walletBId = randomId();
    String transferId = randomId();
    InitialBalance initWalletA = InitialBalance.newBuilder().setWalletId(walletAId).setBalance(100).build();
    walletClient.create(initWalletA).toCompletableFuture().get(5, SECONDS);

    Transfer transfer = Transfer.newBuilder().setTransferId(transferId).setFrom(walletAId).setTo(walletBId).setAmount(20).build();
    workflowClient.start(transfer).toCompletableFuture().get(5, SECONDS);


    await().atMost(10, SECONDS).untilAsserted(() -> {

      TransferState transferState = workflowClient.getTransferState(getTransferRequest(transferId)).toCompletableFuture().get(5, SECONDS);
      assertEquals(COMPENSATION_COMPLETED, transferState.getStatus());

      WalletState walletA = walletClient.getWalletState(GetRequest.newBuilder().setWalletId(walletAId).build())
        .toCompletableFuture().get(5, SECONDS);

      assertEquals(100, walletA.getBalance());
    });
  }


  @Test
  public void shouldTimedOutTransferWorkflow() throws ExecutionException, InterruptedException, TimeoutException {
    String walletAId = randomId();
    String walletBId = randomId();
    String transferId = randomId();

    Transfer transfer = Transfer.newBuilder().setTransferId(transferId).setFrom(walletAId).setTo(walletBId).setAmount(20).build();
    workflowClient.start(transfer).toCompletableFuture().get(5, SECONDS);


    await().ignoreExceptions().atMost(10, SECONDS).untilAsserted(() -> {

      TransferState transferState = workflowClient.getTransferState(getTransferRequest(transferId)).toCompletableFuture().get(5, SECONDS);
      assertEquals(REQUIRES_MANUAL_INTERVENTION, transferState.getStatus());

    });
  }

  @NotNull
  private static TransferApi.GetRequest getTransferRequest(String transferId) {
    TransferApi.GetRequest getRequest = TransferApi.GetRequest.newBuilder().setTransferId(transferId).build();
    return getRequest;
  }

  private String randomId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
