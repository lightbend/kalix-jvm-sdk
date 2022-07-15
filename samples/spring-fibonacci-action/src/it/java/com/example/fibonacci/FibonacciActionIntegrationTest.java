package com.example.fibonacci;

import com.example.Main;
import kalix.springsdk.KalixConfigurationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class FibonacciActionIntegrationTest {


  @Autowired
  private WebClient webClient;

  @Test
  public void calculateNextNumber() throws Exception {

    Mono<Number> response =
        webClient.get()
            .uri("/fibonacci/5/next")
            .retrieve().bodyToMono(Number.class);

    long next = response.block(Duration.of(5, SECONDS)).value;
    Assertions.assertEquals(8, next);

  }

  @Test
  public void wrongNumberReturnsError() throws Exception {
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
}
