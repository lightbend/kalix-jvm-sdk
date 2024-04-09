/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import com.example.wiring.TestkitConfig;
import com.example.wiring.actions.echo.Message;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.client.ComponentClient;
import kalix.spring.KalixConfigurationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = Main.class)
@Import({KalixConfigurationTest.class, TestkitConfig.class})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class SpringWorkflowIntegrationTest {

  @Autowired
  private WebClient webClient;

  @Autowired
  private ComponentClient componentClient;

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
    var transfer = new Transfer(walletId1, walletId2, 10);

    Message response = execute(componentClient.forWorkflow(transferId)
        .call(TransferWorkflow::startTransfer)
        .params(transfer));

    assertThat(response.text()).isEqualTo("transfer started");

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
    var transfer = new Transfer(walletId1, walletId2, 10);

    Message response = execute(componentClient.forWorkflow(transferId)
        .call(TransferWorkflowWithoutInputs::startTransfer)
        .params(transfer));

    assertThat(response.text()).isEqualTo("transfer started");

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
    var transfer = new Transfer(walletId1, walletId2, 10);

    Message response = execute(componentClient.forWorkflow(transferId)
        .call(TransferWorkflowWithoutInputs::startTransferAsync)
        .params(transfer));

    assertThat(response.text()).isEqualTo("transfer started");

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
    var transfer = new Transfer(walletId1, walletId2, 10);

    Message response = execute(componentClient.forWorkflow(transferId)
        .call(TransferWorkflowWithFraudDetection::startTransfer)
        .params(transfer));

    assertThat(response.text()).isEqualTo("transfer started");

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
    var transfer = new Transfer(walletId1, walletId2, 1000);

    Message response = execute(componentClient.forWorkflow(transferId)
        .call(TransferWorkflowWithFraudDetection::startTransfer)
        .params(transfer));

    assertThat(response.text()).isEqualTo("transfer started");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {

          var transferState = execute(componentClient.forWorkflow(transferId).call(TransferWorkflowWithFraudDetection::getTransferState));
          assertThat(transferState.finished).isFalse();
          assertThat(transferState.accepted).isFalse();
          assertThat(transferState.lastStep).isEqualTo("fraud-detection");
        });

    Message acceptedResponse = execute(componentClient.forWorkflow(transferId)
        .call(TransferWorkflowWithFraudDetection::acceptTransfer));

    assertThat(acceptedResponse.text()).isEqualTo("transfer accepted");


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
    var transfer = new Transfer(walletId1, walletId2, 1000000);

    Message response = execute(componentClient.forWorkflow(transferId)
        .call(TransferWorkflowWithFraudDetection::startTransfer)
        .params(transfer));

    assertThat(response.text()).isEqualTo("transfer started");

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var balance1 = getWalletBalance(walletId1);
          var balance2 = getWalletBalance(walletId2);

          assertThat(balance1).isEqualTo(100);
          assertThat(balance2).isEqualTo(100);

          var transferState = execute(componentClient.forWorkflow(transferId).call(TransferWorkflowWithFraudDetection::getTransferState));
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

    //when
    Message response = execute(componentClient.forWorkflow(workflowId)
        .call(WorkflowWithDefaultRecoverStrategy::startFailingCounter)
        .params(counterId));

    assertThat(response.text()).isEqualTo("workflow started");

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          Integer counterValue = getFailingCounterValue(counterId);
          assertThat(counterValue).isEqualTo(3);
        });

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var state = execute(componentClient.forWorkflow(workflowId).call(WorkflowWithDefaultRecoverStrategy::get));
          assertThat(state.finished()).isTrue();
        });
  }

  @Test
  public void shouldRecoverFailingCounterWorkflowWithRecoverStrategy() {
    //given
    var counterId = randomId();
    var workflowId = randomId();

    //when
    Message response = execute(componentClient.forWorkflow(workflowId)
        .call(WorkflowWithRecoverStrategy::startFailingCounter)
        .params(counterId));

    assertThat(response.text()).isEqualTo("workflow started");

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          Integer counterValue = getFailingCounterValue(counterId);
          assertThat(counterValue).isEqualTo(3);
        });

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var state = execute(componentClient.forWorkflow(workflowId).call(WorkflowWithRecoverStrategy::get));
          assertThat(state.finished()).isTrue();
        });
  }

  @Test
  public void shouldRecoverFailingCounterWorkflowWithRecoverStrategyAndAsyncCall() {
    //given
    var counterId = randomId();
    var workflowId = randomId();

    //when
    Message response = execute(componentClient.forWorkflow(workflowId)
        .call(WorkflowWithRecoverStrategyAndAsyncCall::startFailingCounter)
        .params(counterId));

    assertThat(response.text()).isEqualTo("workflow started");

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .ignoreExceptions()
        .untilAsserted(() -> {
          Integer counterValue = getFailingCounterValue(counterId);
          assertThat(counterValue).isEqualTo(3);
        });

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .ignoreExceptions()
        .untilAsserted(() -> {
          var state = execute(componentClient.forWorkflow(workflowId).call(WorkflowWithRecoverStrategyAndAsyncCall::get));
          assertThat(state.finished()).isTrue();
        });
  }

  @Test
  public void shouldRecoverWorkflowTimeout() {
    //given
    var counterId = randomId();
    var workflowId = randomId();

    //when
    Message response = execute(componentClient.forWorkflow(workflowId)
        .call(WorkflowWithTimeout::startFailingCounter)
        .params(counterId));

    assertThat(response.text()).isEqualTo("workflow started");

    //then
    await()
        .atMost(15, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          Integer counterValue = getFailingCounterValue(counterId);
          assertThat(counterValue).isEqualTo(3);
        });

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var state = execute(componentClient.forWorkflow(workflowId).call(WorkflowWithTimeout::get));
          assertThat(state.finished()).isTrue();
        });
  }

  @Test
  public void shouldRecoverWorkflowStepTimeout() {
    //given
    var counterId = randomId();
    var workflowId = randomId();

    //when
    Message response = execute(componentClient.forWorkflow(workflowId)
        .call(WorkflowWithStepTimeout::startFailingCounter)
        .params(counterId));

    assertThat(response.text()).isEqualTo("workflow started");

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .ignoreExceptions()
        .untilAsserted(() -> {
          var state = execute(componentClient.forWorkflow(workflowId).call(WorkflowWithStepTimeout::get));
          assertThat(state.value()).isEqualTo(2);
          assertThat(state.finished()).isTrue();
        });
  }

  @Test
  public void shouldUseTimerInWorkflowDefinition() {
    //given
    var counterId = randomId();
    var workflowId = randomId();

    //when
    Message response = execute(componentClient.forWorkflow(workflowId)
        .call(WorkflowWithTimer::startFailingCounter)
        .params(counterId));

    assertThat(response.text()).isEqualTo("workflow started");

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var state = execute(componentClient.forWorkflow(workflowId).call(WorkflowWithTimer::get));
          assertThat(state.finished()).isTrue();
          assertThat(state.value()).isEqualTo(12);
        });
  }

  @Test
  public void failRequestWhenReqParamsIsNotPresent() {
    //given
    var workflowId = randomId();
    String path = "/workflow-with-timer/" + workflowId;

    //when
    ResponseEntity<String> response = webClient.put().uri(path)
      .retrieve()
      .toEntity(String.class)
      .onErrorResume(WebClientResponseException.class, error -> Mono.just(ResponseEntity.status(error.getStatusCode()).body(error.getResponseBodyAsString())))
      .block(timeout);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("Required request parameter is missing: counterId");
  }

  @Test
  public void shouldNotUpdateWorkflowStateAfterEndTransition() {
    //given
    var workflowId = randomId();
    execute(componentClient.forWorkflow(workflowId).call(DummyWorkflow::startAndFinish));
    assertThat(execute(componentClient.forWorkflow(workflowId).call(DummyWorkflow::get))).isEqualTo(10);

    //when
    try {
      execute(componentClient.forWorkflow(workflowId).call(DummyWorkflow::update));
    } catch (RuntimeException exception) {
      // ignore "500 Internal Server Error" exception from the proxy
    }

    //then
    assertThat(execute(componentClient.forWorkflow(workflowId).call(DummyWorkflow::get))).isEqualTo(10);
  }

  @Test
  public void shouldRunWorkflowStepWithoutInitialState() {
    //given
    var workflowId = randomId();

    //when
    String response = execute(componentClient.forWorkflow(workflowId)
        .call(WorkflowWithoutInitialState::start));

    assertThat(response).contains("ok");

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var state = execute(componentClient.forWorkflow(workflowId).call(WorkflowWithoutInitialState::get));
          assertThat(state).contains("success");
        });
  }

  private <T> T execute(DeferredCall<Any, T> deferredCall) {
    return execute(deferredCall, timeout);
  }

  private <T> T execute(DeferredCall<Any, T> deferredCall, Duration timeout) {
    try {
      return deferredCall.execute().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private String randomTransferId() {
    return randomId();
  }

  private static String randomId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private Integer getFailingCounterValue(String counterId) {
    return execute(componentClient.forEventSourcedEntity(counterId)
        .call(FailingCounterEntity::get), Duration.ofSeconds(20));
  }

  private void createWallet(String walletId, int amount) {
    execute(componentClient.forValueEntity(walletId)
        .call(WalletEntity::create)
        .params(amount));
  }

  private int getWalletBalance(String walletId) {
    return execute(componentClient.forValueEntity(walletId)
        .call(WalletEntity::get)).value;
  }
}
