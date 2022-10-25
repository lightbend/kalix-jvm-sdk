package com.example;

import com.example.Main;
import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;


/**
 * This is a skeleton for implmenting integration tests for a Kalix application built with the Spring SDK.
 *
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
// tag::it[]
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class IntegrationTest extends KalixIntegrationTestKitSupport { // <1>

  @Autowired
  private WebClient webClient; // <2>

  @Test
  public void test() throws Exception {
    // implement your integration tests here by calling your
    // REST endpoints using the provided WebClient
  }
}
// end::it[]