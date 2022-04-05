package com.example.client.spring.rest;

import com.example.client.spring.rest.model.ValueRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CounterControllerIntegrationTest {

  WireMockServer wireMockServer;

  @Autowired
  WebTestClient webTestClient;

  @BeforeEach
  void setUp() throws Exception {
    wireMockServer = new WireMockServer(9000);
    wireMockServer.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    wireMockServer.stop();
  }

  @Test
  @DisplayName("Get Counter Should Return Success When Server Returns Success")
  void testGetCounter() {

    wireMockServer.stubFor(WireMock.post("/com.example.CounterService/GetCurrentCounter")
        .willReturn(ok("{}")));

    webTestClient.get()
        .uri("/counter/{counterId}", "test")
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .isEqualTo("{}");
  }

  @Test
  @DisplayName("Get Counter Should Return Error When Server Returns Error")
  void testGetCounterWithError() {

    wireMockServer.stubFor(WireMock.post("/com.example.CounterService/GetCurrentCounter")
        .willReturn(serverError()));

    webTestClient.get()
        .uri("/counter/{counterId}", "test")
        .exchange()
        .expectStatus().is5xxServerError();
  }

  @Test
  @DisplayName("Increase Counter Should Return Success When Server Returns Success")
  public void testIncreaseCounter() {

    wireMockServer.stubFor(WireMock.post("/com.example.CounterService/Increase")
        .willReturn(ok("{}")));

    ValueRequest valueRequest = new ValueRequest();
    valueRequest.setValue(1);

    webTestClient.post()
        .uri("/counter/{counterId}/increase", "test")
        .bodyValue(valueRequest)
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  @DisplayName("Increase Counter Should Return Error When Server Returns Error")
  public void testIncreaseCounterWithError() {

    wireMockServer.stubFor(WireMock.post("/com.example.CounterService/Increase")
        .willReturn(serverError()));

    ValueRequest valueRequest = new ValueRequest();
    valueRequest.setValue(1);

    webTestClient.post()
        .uri("/counter/{counterId}/increase", "test")
        .bodyValue(valueRequest)
        .exchange()
        .expectStatus().is5xxServerError();
  }

  @Test
  @DisplayName("Reset Counter Should Return Success When Server Returns Success")
  public void testResetCounter() {

    wireMockServer.stubFor(WireMock.post("/com.example.CounterService/Reset")
        .willReturn(ok("{}")));

    ValueRequest valueRequest = new ValueRequest();
    valueRequest.setValue(1);

    webTestClient.post()
        .uri("/counter/{counterId}/reset", "test")
        .bodyValue(valueRequest)
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  @DisplayName("Reset Counter Should Return Error When Server Returns Error")
  public void testResetCounterWithError() {

    wireMockServer.stubFor(WireMock.post("/com.example.CounterService/Reset")
        .willReturn(ok("{}")));

    ValueRequest valueRequest = new ValueRequest();
    valueRequest.setValue(1);

    webTestClient.post()
        .uri("/counter/{counterId}/reset", "test")
        .bodyValue(valueRequest)
        .exchange()
        .expectStatus().isOk();
  }

}