package com.example;

import com.example.transfer.Message;
import com.example.transfer.Transfer;
import com.example.transfer.TransferState;
import com.example.wallet.Balance;
import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.transfer.TransferStatus.COMPLETED;
import static com.example.transfer.TransferStatus.MANUAL_APPROVAL_REQUIRED;
import static com.example.transfer.TransferStatus.REJECTED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class TransferWorkflowIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void shouldTransferMoney() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 10);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(Message::value)
        .block(timeout);

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
  public void shouldRejectTransfer() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 1_000_001);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(Message::value)
        .block(timeout);

    assertThat(response).isEqualTo("transfer started");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var balance1 = getWalletBalance(walletId1);
          var balance2 = getWalletBalance(walletId2);

          assertThat(balance1).isEqualTo(100);
          assertThat(balance2).isEqualTo(100);

          TransferState transferState = getTransferState(transferUrl);
          assertThat(transferState.finished()).isTrue();
          assertThat(transferState.status()).isEqualTo(REJECTED);
        });
  }

  @Test
  public void shouldConfirmTransfer() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 1001);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 1_001);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(Message::value)
        .block(timeout);

    assertThat(response).isEqualTo("transfer started");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          TransferState transferState = getTransferState(transferUrl);
          assertThat(transferState.finished()).isFalse();
          assertThat(transferState.status()).isEqualTo(MANUAL_APPROVAL_REQUIRED);
        });

    String acceptationResponse = webClient.patch().uri(transferUrl + "/accept")
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(Message::value)
        .block(timeout);

    assertThat(acceptationResponse).isEqualTo("transfer accepted");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var balance1 = getWalletBalance(walletId1);
          var balance2 = getWalletBalance(walletId2);

          assertThat(balance1).isEqualTo(0);
          assertThat(balance2).isEqualTo(1101);

          TransferState transferState = getTransferState(transferUrl);
          assertThat(transferState.finished()).isTrue();
          assertThat(transferState.status()).isEqualTo(COMPLETED);
        });
  }

  private String randomTransferId(){
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private void createWallet(String walletId, int amount) {
    ResponseEntity<Void> response = webClient.post().uri("/wallet/" + walletId + "/create/" + amount)
        .retrieve()
        .toBodilessEntity()
        .block(timeout);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private int getWalletBalance(String walletId) {
    Balance response = webClient.get().uri("/wallet/" + walletId)
        .retrieve()
        .bodyToMono(Balance.class)
        .block(timeout);

    return response.value();
  }

  private TransferState getTransferState(String url) {
    return webClient.get().uri(url)
        .retrieve()
        .bodyToMono(TransferState.class)
        .block(timeout);
  }
}