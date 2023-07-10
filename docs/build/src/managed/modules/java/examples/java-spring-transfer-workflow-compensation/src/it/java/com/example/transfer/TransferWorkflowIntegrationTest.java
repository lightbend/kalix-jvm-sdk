package com.example.transfer;

import com.example.Main;
import com.example.transfer.TransferState.Transfer;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.transfer.TransferState.TransferStatus.COMPENSATION_COMPLETED;
import static com.example.transfer.TransferState.TransferStatus.REQUIRES_MANUAL_INTERVENTION;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@SpringBootTest(classes = Main.class)
public class TransferWorkflowIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void shouldTransferMoney() {
    var walletId1 = randomId();
    var walletId2 = randomId();
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomId();
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
  public void shouldCompensateFailedMoneyTransfer() {
    var walletId1 = randomId();
    var walletId2 = randomId();
    createWallet(walletId1, 100);
    var transferId = randomId();
    var transferUrl = "/transfer/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 10); //walletId2 not exists

    String response = webClient.put().uri(transferUrl)
      .bodyValue(transfer)
      .retrieve()
      .bodyToMono(Message.class)
      .map(Message::value)
      .block(timeout);

    assertThat(response).isEqualTo("transfer started");

    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        TransferState transferState = getTransferState(transferUrl);
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
    var transferUrl = "/transfer/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 10); //both not exists

    String response = webClient.put().uri(transferUrl)
      .bodyValue(transfer)
      .retrieve()
      .bodyToMono(Message.class)
      .map(Message::value)
      .block(timeout);

    assertThat(response).isEqualTo("transfer started");

    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        TransferState transferState = getTransferState(transferUrl);
        assertThat(transferState.status()).isEqualTo(REQUIRES_MANUAL_INTERVENTION);
      });
  }


  private String randomId() {
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
    Integer response = webClient.get().uri("/wallet/" + walletId)
      .retrieve()
      .bodyToMono(Integer.class)
      .block(timeout);

    return response;
  }

  private TransferState getTransferState(String url) {
    return webClient.get().uri(url)
      .retrieve()
      .bodyToMono(TransferState.class)
      .block(timeout);
  }
}