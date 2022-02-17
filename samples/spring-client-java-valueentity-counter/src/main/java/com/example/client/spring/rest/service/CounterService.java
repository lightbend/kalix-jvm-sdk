package com.example.client.spring.rest.service;

import com.example.client.spring.rest.model.CounterRequest;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public interface CounterService {

    Mono<String> getCurrentCounter(CounterRequest request, MultiValueMap<String, String> requestHeaders);

    Mono<String> increase(CounterRequest request, MultiValueMap<String, String> requestHeaders);

    Mono<String> reset(CounterRequest request, MultiValueMap<String, String> requestHeaders);

}
