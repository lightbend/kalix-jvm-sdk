package com.example.client.spring.rest.controller;

import com.example.client.spring.rest.model.CounterRequest;
import com.example.client.spring.rest.service.CounterService;
import com.example.client.spring.rest.service.GrpcClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class CounterController {

  @Autowired
  CounterService counterService;

  @Autowired
  GrpcClientService grpcClientService;

  @PostMapping(path = "/getCurrentCounter")
  public Mono<String> getCurrentCounter(@RequestBody CounterRequest input, @RequestHeader MultiValueMap<String, String> requestHeaders) {
    return counterService.getCurrentCounter(input, requestHeaders);
  }

  @PostMapping(path = "/increase")
  public Mono<String> increaseCounter(@RequestBody CounterRequest input, @RequestHeader MultiValueMap<String, String> requestHeaders) {
    return counterService.increase(input, requestHeaders);
  }

  @PostMapping(path = "/decrease")
  public String decreaseCounter(@RequestBody CounterRequest input) {
    return grpcClientService.decrease(input);
  }

  @PostMapping(path = "/reset")
  public Mono<String> resetCounter(@RequestBody CounterRequest input, @RequestHeader MultiValueMap<String, String> requestHeaders) {
    return counterService.reset(input, requestHeaders);
  }
}
