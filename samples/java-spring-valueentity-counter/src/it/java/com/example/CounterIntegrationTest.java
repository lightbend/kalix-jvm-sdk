package com.example;

import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;


/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Java SDK.
 *
 * This test will initiate a Kalix Runtime using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */

// tag::sample-it[]
@SpringBootTest(classes = Main.class)
public class CounterIntegrationTest extends KalixIntegrationTestKitSupport { // <1>

  @Autowired
  private WebClient webClient; // <2>

  private Duration timeout = Duration.of(10, SECONDS);

  // end::sample-it[]
  @Test
  public void verifyCounterIncrease() {

    Number counterIncrease =
        webClient
            .post()
            .uri("/counter/foo/increase")
            .bodyValue(new Number(10))
            .retrieve()
            .bodyToMono(Number.class)
            .block(timeout);

    Assertions.assertEquals(10, counterIncrease.value());
  }

  // tag::sample-it[]
  @Test
  public void verifyCounterSetAndIncrease() {

    Number counterGet = // <3>
        webClient
            .get()
            .uri("/counter/bar")
            .retrieve()
            .bodyToMono(Number.class)
            .block(timeout);
    Assertions.assertEquals(0, counterGet.value());

    Number counterPlusOne = // <4>
        webClient
            .post()
            .uri("/counter/bar/plusone")
            .retrieve()
            .bodyToMono(Number.class)
            .block(timeout);

    Assertions.assertEquals(1, counterPlusOne.value());

    Number counterGetAfter = // <5>
        webClient
            .get()
            .uri("/counter/bar")
            .retrieve()
            .bodyToMono(Number.class)
            .block(timeout);
    Assertions.assertEquals(1, counterGetAfter.value());
  }
}
// end::sample-it[]
