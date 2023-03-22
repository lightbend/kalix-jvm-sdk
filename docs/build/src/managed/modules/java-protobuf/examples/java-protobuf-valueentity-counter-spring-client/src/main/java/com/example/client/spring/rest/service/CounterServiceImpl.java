package com.example.client.spring.rest.service;

import com.example.client.spring.rest.model.CounterRequest;
import com.example.client.spring.rest.model.ValueRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// tag::getCurrentCounterCall[]
@Service
public class CounterServiceImpl implements CounterService {

  @Autowired
  WebClient webClient;

  @Override
  public Mono<String> getCounter(String counterId) {

    CounterRequest counterRequest = new CounterRequest();
    counterRequest.setCounterId(counterId);

    return webClient.post()
        .uri("/com.example.CounterService/GetCurrentCounter")
        .bodyValue(counterRequest)
        .retrieve()
        .bodyToMono(String.class);
  }
  // end::getCurrentCounterCall[]

  @Override
  public Mono<String> increaseCounter(String counterId, ValueRequest request, MultiValueMap<String, String> requestHeaders) {

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
  public Mono<String> resetCounter(String counterId) {

    CounterRequest counterRequest = new CounterRequest();
    counterRequest.setCounterId(counterId);

    return webClient.post()
        .uri("/com.example.CounterService/Reset")
        .bodyValue(counterRequest)
        .retrieve()
        .bodyToMono(String.class);
  }
}
