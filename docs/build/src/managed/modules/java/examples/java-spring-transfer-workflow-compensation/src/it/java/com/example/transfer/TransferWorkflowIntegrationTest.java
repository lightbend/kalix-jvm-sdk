package com.example.transfer;

import com.example.Main;
import com.example.transfer.TransferState.Transfer;
import com.example.wallet.WalletEntity;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.transfer.TransferState.TransferStatus.COMPENSATION_COMPLETED;
import static com.example.transfer.TransferState.TransferStatus.REQUIRES_MANUAL_INTERVENTION;
import static com.example.transfer.TransferState.TransferStatus.TRANSFER_ACCEPTATION_TIMED_OUT;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@SpringBootTest(classes = Main.class)
public class TransferWorkflowIntegrationTest extends KalixIntegrationTestKitSupport {

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void shouldTransferMoney() {
    var walletId1 = randomId();
    var walletId2 = randomId();
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomId();
    var transfer = new Transfer(walletId1, walletId2, 10);

    String response = execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::startTransfer)
      .params(transfer))
      .value();

    assertThat(response).isEqualTo("transfer started");

    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var balance1 = getWalletBalance(walletId1);
        var balance2 = getWalletBalance(walletId2);

        assertThat(balance1).isEqualTo(90);
        assertThat(balance2).isEqualTo(110);
      });
  }

  @Test
  public void shouldTransferMoneyWithAcceptation() {
    var walletId1 = randomId();
    var walletId2 = randomId();
    createWallet(walletId1, 2000);
    createWallet(walletId2, 100);
    var transferId = randomId();
    var transfer = new Transfer(walletId1, walletId2, 1001);

    String response = execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::startTransfer)
      .params(transfer))
      .value();

    assertThat(response).isEqualTo("transfer started, waiting for acceptation");

    String acceptationResponse = execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::accept))
      .value();

    assertThat(acceptationResponse).isEqualTo("transfer accepted");

    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var balance1 = getWalletBalance(walletId1);
        var balance2 = getWalletBalance(walletId2);

        assertThat(balance1).isEqualTo(999);
        assertThat(balance2).isEqualTo(1101);
      });
  }

  @Test
  public void shouldTimeoutTransferAcceptation() {
    var walletId1 = randomId();
    var walletId2 = randomId();
    createWallet(walletId1, 2000);
    createWallet(walletId2, 100);
    var transferId = randomId();
    var transfer = new Transfer(walletId1, walletId2, 1001);

    String response = execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::startTransfer)
      .params(transfer))
      .value();
    assertThat(response).isEqualTo("transfer started, waiting for acceptation");

    String acceptationResponse = execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::acceptationTimeout));
    assertThat(acceptationResponse).contains("timed out");

    var balance1 = getWalletBalance(walletId1);
    var balance2 = getWalletBalance(walletId2);
    assertThat(balance1).isEqualTo(2000);
    assertThat(balance2).isEqualTo(100);

    TransferState transferState = getTransferState(transferId);
    assertThat(transferState.status()).isEqualTo(TRANSFER_ACCEPTATION_TIMED_OUT);
  }

  @Test
  public void shouldCompensateFailedMoneyTransfer() {
    var walletId1 = randomId();
    var walletId2 = randomId();
    createWallet(walletId1, 100);
    var transferId = randomId();
    var transfer = new Transfer(walletId1, walletId2, 10); //walletId2 not exists

    String response = execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::startTransfer)
      .params(transfer))
      .value();

    assertThat(response).isEqualTo("transfer started");

    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        TransferState transferState = getTransferState(transferId);
        assertThat(transferState.status()).isEqualTo(COMPENSATION_COMPLETED);

        var balance1 = getWalletBalance(walletId1);

        assertThat(balance1).isEqualTo(100);
      });
  }

  @Test
  public void shouldTimedOutTransferWorkflow() {
    var walletId1 = randomId();
    var walletId2 = randomId();
    var transferId = randomId();
    var transfer = new Transfer(walletId1, walletId2, 10); //both not exists

    String response = execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::startTransfer)
      .params(transfer))
      .value();

    assertThat(response).isEqualTo("transfer started");

    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        TransferState transferState = getTransferState(transferId);
        assertThat(transferState.status()).isEqualTo(REQUIRES_MANUAL_INTERVENTION);
      });
  }


  private String randomId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private void createWallet(String walletId, int amount) {
    String response = execute(componentClient
      .forValueEntity(walletId)
      .call(WalletEntity::create)
      .params(walletId, amount));

    assertThat(response).contains("Ok");
  }

  private int getWalletBalance(String walletId) {
    return execute(componentClient
      .forValueEntity(walletId)
      .call(WalletEntity::get));
  }

  private TransferState getTransferState(String transferId) {
    return execute(componentClient
      .forWorkflow(transferId)
      .call(TransferWorkflow::getTransferState));
  }

  private <T> T execute(DeferredCall<Any, T> deferredCall) {
    try {
      return deferredCall.execute().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}