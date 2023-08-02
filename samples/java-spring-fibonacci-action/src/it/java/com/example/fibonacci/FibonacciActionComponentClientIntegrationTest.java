package com.example.fibonacci;

import com.example.Main;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@DirtiesContext
// tag::testing-action[]
@SpringBootTest(classes = Main.class)
public class FibonacciActionComponentClientIntegrationTest extends KalixIntegrationTestKitSupport {

  private Duration timeout = Duration.of(5, SECONDS);

  @Test
  public void calculateNextNumber() throws ExecutionException, InterruptedException, TimeoutException {

    Number response = componentClient.forAction() // <1>
        .call(FibonacciAction::nextNumber)
        .params(new Number(5))
        .execute() // <2>
        .toCompletableFuture()
        .get(timeout.toMillis(), MILLISECONDS);

    Assertions.assertEquals(8, response.value());
  }
  // end::testing-action[]

  @Test
  public void calculateNextNumberWithLimitedFibo() throws ExecutionException, InterruptedException, TimeoutException {

    Number response = componentClient.forAction()
        .call(LimitedFibonacciAction::nextNumber)
        .params(new Number(5))
        .execute()
        .toCompletableFuture()
        .get(timeout.toMillis(), MILLISECONDS);

    Assertions.assertEquals(8, response.value());
  }

  // tag::testing-action[]
}
// end::testing-action[]
