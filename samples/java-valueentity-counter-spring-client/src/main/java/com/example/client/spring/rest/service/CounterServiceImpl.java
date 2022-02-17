package com.example.client.spring.rest.service;

import com.example.client.spring.rest.model.CounterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class CounterServiceImpl implements CounterService {

  @Autowired
  WebClient webClient;

  @Override
  public Mono<String> getCurrentCounter(CounterRequest request, MultiValueMap<String, String> requestHeaders) {
    return webClient.post()
        .uri("/com.example.CounterService/GetCurrentCounter")
        .headers(httpHeaders -> httpHeaders.setAll(requestHeaders.toSingleValueMap()))
        .bodyValue(request)
        .retrieve()
        .bodyToMono(String.class);
  }

  @Override
  public Mono<String> increase(CounterRequest request, MultiValueMap<String, String> requestHeaders) {
    return webClient.post()
        .uri("/com.example.CounterService/Increase")
        .headers(httpHeaders -> httpHeaders.setAll(requestHeaders.toSingleValueMap()))
        .bodyValue(request)
        .retrieve()
        .bodyToMono(String.class);
  }

  @Override
  public Mono<String> reset(CounterRequest request, MultiValueMap<String, String> requestHeaders) {
    return webClient.post()
        .uri("/com.example.CounterService/Reset")
        .headers(httpHeaders -> httpHeaders.setAll(requestHeaders.toSingleValueMap()))
        .bodyValue(request)
        .retrieve()
        .bodyToMono(String.class);
  }
}
