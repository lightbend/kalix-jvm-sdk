package com.example.client.spring.rest.service;

import com.example.client.spring.rest.model.CounterRequest;
import com.example.client.spring.rest.model.ValueRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class CounterServiceImpl implements CounterService {

  // tag::getCurrentCounterCall[]
  @Autowired
  WebClient webClient;

  @Override
  public Mono<String> getCurrentCounter(MultiValueMap<String, String> requestHeaders, String counterId) {

    CounterRequest counterRequest = new CounterRequest();
    counterRequest.setCounterId(counterId);

    return webClient.post()
        .uri("/com.example.CounterService/GetCurrentCounter")
        .headers(httpHeaders -> httpHeaders.setAll(requestHeaders.toSingleValueMap()))
        .bodyValue(counterRequest)
        .retrieve()
        .bodyToMono(String.class);
  }
  // end::getCurrentCounterCall[]

  @Override
  public Mono<String> increase(String counterId, ValueRequest request, MultiValueMap<String, String> requestHeaders) {

    CounterRequest counterRequest = new CounterRequest();
    counterRequest.setCounterId(counterId);
    counterRequest.setValue(request.getValue());

    return webClient.post()
        .uri("/com.example.CounterService/Increase")
        .headers(httpHeaders -> httpHeaders.setAll(requestHeaders.toSingleValueMap()))
        .bodyValue(counterRequest)
        .retrieve()
        .bodyToMono(String.class);
  }

  @Override
  public Mono<String> reset(String counterId, MultiValueMap<String, String> requestHeaders) {

    CounterRequest counterRequest = new CounterRequest();
    counterRequest.setCounterId(counterId);

    return webClient.post()
        .uri("/com.example.CounterService/Reset")
        .headers(httpHeaders -> httpHeaders.setAll(requestHeaders.toSingleValueMap()))
        .bodyValue(counterRequest)
        .retrieve()
        .bodyToMono(String.class);
  }
}
