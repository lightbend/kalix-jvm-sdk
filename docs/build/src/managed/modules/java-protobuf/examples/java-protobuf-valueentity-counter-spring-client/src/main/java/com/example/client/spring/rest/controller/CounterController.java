package com.example.client.spring.rest.controller;

import com.example.client.spring.rest.model.ValueRequest;
import com.example.client.spring.rest.service.CounterService;
import com.example.client.spring.rest.service.GrpcClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

// tag::getCounterEndpoint[]
@RestController
@RequestMapping("/counter")
public class CounterController {

  @Autowired
  CounterService counterService;

  @Autowired
  GrpcClientService grpcClientService;

  @GetMapping(path = "/{counterId}")
  public Mono<String> getCounter(@PathVariable String counterId){
    return counterService.getCounter(counterId);
  }
  // end::getCounterEndpoint[]

  @PostMapping(path = "/{counterId}/increase")
  public Mono<String> increaseCounter(@PathVariable String counterId, @RequestBody ValueRequest input, @RequestHeader MultiValueMap<String, String> requestHeaders) {
    return counterService.increaseCounter(counterId, input, requestHeaders);
  }

  @PostMapping(path = "/{counterId}/decrease")
  public String decreaseCounter(@PathVariable String counterId, @RequestBody ValueRequest input) {
    return grpcClientService.decreaseCounter(counterId, input);
  }

  @PostMapping(path = "/{counterId}/reset")
  public Mono<String> resetCounter(@PathVariable String counterId) {
    return counterService.resetCounter(counterId);
  }
}
