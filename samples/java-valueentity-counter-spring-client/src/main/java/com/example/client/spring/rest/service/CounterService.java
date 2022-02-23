package com.example.client.spring.rest.service;

import com.example.client.spring.rest.model.ValueRequest;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public interface CounterService {

  Mono<String> getCurrentCounter(MultiValueMap<String, String> requestHeaders, String counterId);

  Mono<String> increase(String counterId, ValueRequest request, MultiValueMap<String, String> requestHeaders);

  Mono<String> reset(String counterId, MultiValueMap<String, String> requestHeaders);

}
