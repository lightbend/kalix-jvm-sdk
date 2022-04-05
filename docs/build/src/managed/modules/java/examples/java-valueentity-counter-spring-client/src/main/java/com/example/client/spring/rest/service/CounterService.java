package com.example.client.spring.rest.service;

import com.example.client.spring.rest.model.ValueRequest;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public interface CounterService {

  Mono<String> getCounter(String counterId);

  Mono<String> increaseCounter(String counterId, ValueRequest request, MultiValueMap<String, String> requestHeaders);

  Mono<String> resetCounter(String counterId);

}
