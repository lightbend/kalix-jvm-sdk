package com.example.client.spring.rest;

import com.example.client.spring.rest.controller.CounterController;
import com.example.client.spring.rest.model.ValueRequest;
import com.example.client.spring.rest.service.CounterService;
import com.example.client.spring.rest.service.GrpcClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(CounterController.class)
public class CounterControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  CounterService counterService;

  @MockBean
  GrpcClientService grpcClientService;


  @Test
  public void testGetCurrentCounter() {

    when(counterService.getCounter(any())).thenReturn(Mono.just("{}"));

    webTestClient.get()
        .uri("/counter/{counterId}", "test")
        .exchange()
        .expectStatus().isOk();
  }


  @Test
  public void testIncreaseCounter() {

    ValueRequest valueRequest = new ValueRequest();
    valueRequest.setValue(1);

    when(counterService.increaseCounter(any(), any(), any())).thenReturn(Mono.just("{}"));

    webTestClient.post()
        .uri("/counter/{counterId}/increase", "test")
        .bodyValue(valueRequest)
        .exchange()
        .expectStatus().isOk();
  }


  @Test
  public void testDecreaseCounter() {

    ValueRequest valueRequest = new ValueRequest();
    valueRequest.setValue(1);

    when(grpcClientService.decreaseCounter(any(), any())).thenReturn("{}");

    webTestClient.post()
        .uri("/counter/{counterId}/decrease", "test")
        .bodyValue(valueRequest)
        .exchange()
        .expectStatus().isOk();
  }


  @Test
  public void testResetCounter() {

    ValueRequest valueRequest = new ValueRequest();
    valueRequest.setValue(1);
    when(counterService.resetCounter(any())).thenReturn(Mono.just("{}"));

    webTestClient.post()
        .uri("/counter/{counterId}/reset", "test")
        .bodyValue(valueRequest)
        .exchange()
        .expectStatus().isOk();
  }
}
