/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wiring.workflowentities;

import com.example.wiring.TestkitConfig;
import com.example.wiring.actions.echo.Message;
import kalix.spring.KalixConfigurationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class)
@Import({KalixConfigurationTest.class, TestkitConfig.class})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class SpringWorkflowIntegrationTest {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(10, SECONDS);


  @Test
  public void shouldNotStartTransferForWithNegativeAmount() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, -10);

    ResponseEntity<Void> response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
            Mono.empty()
        )
        .toBodilessEntity()
        .block(timeout);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

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
        .map(m -> m.text)
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
  public void shouldTransferMoneyWithoutStepInputs() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer-without-inputs/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 10);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
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
  public void shouldTransferAsyncMoneyWithoutStepInputs() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer-without-inputs/" + transferId + "/async";
    var transfer = new Transfer(walletId1, walletId2, 10);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
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
  public void shouldTransferMoneyWithFraudDetection() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer-with-fraud-detection/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 10);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
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
  public void shouldTransferMoneyWithFraudDetectionAndManualAcceptance() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100000);
    createWallet(walletId2, 100000);
    var transferId = randomTransferId();
    var transferUrl = "/transfer-with-fraud-detection/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 1000);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    assertThat(response).isEqualTo("transfer started");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {

          var transferState = getTransferState(transferUrl);
          assertThat(transferState.finished).isFalse();
          assertThat(transferState.accepted).isFalse();
          assertThat(transferState.lastStep).isEqualTo("fraud-detection");
        });

    String acceptanceResponse = webClient.patch().uri(transferUrl + "/accept")
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    assertThat(acceptanceResponse).isEqualTo("transfer accepted");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var balance1 = getWalletBalance(walletId1);
          var balance2 = getWalletBalance(walletId2);

          assertThat(balance1).isEqualTo(99000);
          assertThat(balance2).isEqualTo(101000);
        });
  }

  @Test
  public void shouldNotTransferMoneyWhenFraudDetectionRejectTransfer() {
    var walletId1 = "1";
    var walletId2 = "2";
    createWallet(walletId1, 100);
    createWallet(walletId2, 100);
    var transferId = randomTransferId();
    var transferUrl = "/transfer-with-fraud-detection/" + transferId;
    var transfer = new Transfer(walletId1, walletId2, 1000000);

    String response = webClient.put().uri(transferUrl)
        .bodyValue(transfer)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    assertThat(response).isEqualTo("transfer started");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var balance1 = getWalletBalance(walletId1);
          var balance2 = getWalletBalance(walletId2);

          assertThat(balance1).isEqualTo(100);
          assertThat(balance2).isEqualTo(100);

          var transferState = getTransferState(transferUrl);
          assertThat(transferState.finished).isTrue();
          assertThat(transferState.accepted).isFalse();
          assertThat(transferState.lastStep).isEqualTo("fraud-detection");
        });
  }

  @Test
  public void shouldRecoverFailingCounterWorkflowWithDefaultRecoverStrategy() {
    //given
    var counterId = randomId();
    var workflowId = randomId();
    String path = "/workflow-with-default-recover-strategy/" + workflowId;

    //when
    String response = webClient.put().uri(path + "/" + counterId)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    assertThat(response).isEqualTo("workflow started");

    //then
    await()
        .atMost(20, TimeUnit.of(SECONDS)) //TODO change it to 10 after bumping proxy-version
        .untilAsserted(() -> {
          Integer counterValue = getFailingCounterValue(counterId);
          assertThat(counterValue).isEqualTo(3);
        });

    var state = getWorkflowState(path);
    assertThat(state.finished()).isTrue();
  }

  @Test
  public void shouldRecoverFailingCounterWorkflowWithRecoverStrategy() {
    //given
    var counterId = randomId();
    var workflowId = randomId();
    String path = "/workflow-with-recover-strategy/" + workflowId;

    //when
    String response = webClient.put().uri(path + "/" + counterId)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    assertThat(response).isEqualTo("workflow started");

    //then
    await()
        .atMost(20, TimeUnit.of(SECONDS)) //TODO change it to 10 after bumping proxy-version
        .untilAsserted(() -> {
          Integer counterValue = getFailingCounterValue(counterId);
          assertThat(counterValue).isEqualTo(3);
        });

    var state = getWorkflowState(path);
    assertThat(state.finished()).isTrue();
  }

  @Test
  public void shouldRecoverWorkflowTimeout() {
    //given
    var counterId = randomId();
    var workflowId = randomId();
    String path = "/workflow-with-timeout/" + workflowId;

    //when
    String response = webClient.put().uri(path + "/" + counterId)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    assertThat(response).isEqualTo("workflow started");

    //then
    await()
        .atMost(25, TimeUnit.of(SECONDS)) //TODO change it to 10 after bumping proxy-version
        .untilAsserted(() -> {
          Integer counterValue = getFailingCounterValue(counterId);
          assertThat(counterValue).isEqualTo(3);
        });

    var state = getWorkflowState(path);
    assertThat(state.finished()).isTrue();
  }

  @Test
  public void shouldRecoverWorkflowStepTimeout() throws InterruptedException {
    //given
    var counterId = randomId();
    var workflowId = randomId();
    String path = "/workflow-with-step-timeout/" + workflowId;

    //when
    String response = webClient.put().uri(path + "/" + counterId)
        .retrieve()
        .bodyToMono(Message.class)
        .map(m -> m.text)
        .block(timeout);

    assertThat(response).isEqualTo("workflow started");

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .ignoreExceptions()
        .untilAsserted(() -> {
          var state = getWorkflowState(path);
          assertThat(state.value()).isEqualTo(2);
          assertThat(state.finished()).isTrue();
        });
  }

  private String randomTransferId() {
    return randomId();
  }

  private static String randomId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private Integer getFailingCounterValue(String counterId) {
    return webClient.get().uri("/failing-counter/" + counterId)
        .retrieve()
        .bodyToMono(String.class)
        .map(s -> s.replace("\"", ""))
        .map(Integer::valueOf)
        .block(Duration.ofSeconds(20));
  }

  private FailingCounterState getWorkflowState(String url) {
    return webClient.get().uri(url)
        .retrieve()
        .bodyToMono(FailingCounterState.class)
        .block(Duration.ofSeconds(20));
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

    return response.value;
  }

  private TransferState getTransferState(String url) {
    return webClient.get().uri(url)
        .retrieve()
        .bodyToMono(TransferState.class)
        .block(timeout);
  }
}
