package com.example.fibonacci;

import com.example.Main;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.client.ComponentClient;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.SECONDS;

@SpringBootTest(classes = Main.class)
public class FibonacciActionIntegrationTest extends KalixIntegrationTestKitSupport {


  @Autowired
  private WebClient webClient;

  @Autowired
  private ComponentClient componentClient;
  private Duration timeout = Duration.of(5, SECONDS);

  @Test
  public void calculateNextNumber() {

    Number response = execute(componentClient.forAction()
        .call(FibonacciAction::getNumber)
        .params(5L));

    Assertions.assertEquals(8, response.value());

  }

  @Test
  public void calculateNextNumberWithLimitedFibo() {

    Mono<Number> response =
            webClient.get()
                    .uri("/limitedfibonacci/5/next")
                    .retrieve().bodyToMono(Number.class);

    long next = response.block(Duration.of(5, SECONDS)).value();
    Assertions.assertEquals(8, next);

  }

  @Test
  public void wrongNumberReturnsError() {
    try {


      ResponseEntity<Number> response =
          webClient.get()
              .uri("/fibonacci/7/next")
              .retrieve().toEntity(Number.class)
              .block(Duration.of(5, SECONDS));

      Assertions.fail("Should have failed");

    } catch (WebClientResponseException.InternalServerError ex) {
      String bodyErrorMessage = ex.getResponseBodyAsString();
      Assertions.assertTrue(bodyErrorMessage.contains("Input number is not a Fibonacci number"));
    }
  }

  private <T> T execute(DeferredCall<Any, T> deferredCall) {
    try {
      return deferredCall.execute().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
