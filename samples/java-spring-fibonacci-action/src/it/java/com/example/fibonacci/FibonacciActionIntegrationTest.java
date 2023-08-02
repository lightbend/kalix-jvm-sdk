package com.example.fibonacci;

import com.example.Main;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

@DirtiesContext
// tag::testing-action[]
@SpringBootTest(classes = Main.class) // <1>
public class FibonacciActionIntegrationTest extends KalixIntegrationTestKitSupport { // <2>

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);

  @Test
  public void calculateNextNumber() {

    ResponseEntity<Number> response = webClient.get()
      .uri("/fibonacci/5/next")
      .retrieve()
      .toEntity(Number.class)
      .block(timeout); // <3>

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals(8, response.getBody().value());
  }

  // end::testing-action[]
  @Test
  public void calculateNextNumberWithLimitedFibo() {

    ResponseEntity<Number> response = webClient.get()
      .uri("/limitedfibonacci/5/next")
      .retrieve()
      .toEntity(Number.class)
      .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals(8, response.getBody().value());
  }

  @Test
  public void wrongNumberReturnsError() {

    Mono<ResponseEntity<Void>> response = webClient.get()
      .uri("/fibonacci/7/next")
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
        clientResponse.bodyToMono(String.class)
          .flatMap(error -> Mono.error(new RuntimeException(error)))
      )
      .toBodilessEntity();

    RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> response.block(timeout));
    Assertions.assertEquals("Input number is not a Fibonacci number, received '7'", exception.getMessage());
  }
  // tag::testing-action[]
}
// end::testing-action[]
