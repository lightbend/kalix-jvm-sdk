package com.example.client.spring.rest;

import com.example.client.spring.rest.controller.CounterController;
import com.example.client.spring.rest.model.CounterRequest;
import com.example.client.spring.rest.service.CounterService;
import com.example.client.spring.rest.service.GrpcClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
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

        CounterRequest counterRequest = new CounterRequest();
        counterRequest.setCounterId("test");

        when(counterService.getCurrentCounter(any(), any())).thenReturn(Mono.just("{}"));

        webTestClient.post()
                .uri("/getCurrentCounter")
                .bodyValue(counterRequest)
                .exchange()
                .expectStatus().isOk();
    }


    @Test
    public void testIncreaseCounter() {

        CounterRequest counterRequest = new CounterRequest();
        counterRequest.setCounterId("test");
        counterRequest.setValue(1);

        when(counterService.increase(any(), any())).thenReturn(Mono.just("{}"));

        webTestClient.post()
                .uri("/increase")
                .bodyValue(counterRequest)
                .exchange()
                .expectStatus().isOk();
    }


    @Test
    public void testDecreaseCounter() {

        CounterRequest counterRequest = new CounterRequest();
        counterRequest.setCounterId("test");
        counterRequest.setValue(1);

        when(grpcClientService.decrease(any())).thenReturn("{}");

        webTestClient.post()
                .uri("/decrease")
                .bodyValue(counterRequest)
                .exchange()
                .expectStatus().isOk();
    }


    @Test
    public void testResetCounter() {

        CounterRequest counterRequest = new CounterRequest();
        counterRequest.setCounterId("test");
        counterRequest.setValue(1);

        when(counterService.reset(any(), any())).thenReturn(Mono.just("{}"));

        webTestClient.post()
                .uri("/increase")
                .bodyValue(counterRequest)
                .exchange()
                .expectStatus().isOk();
    }
}
