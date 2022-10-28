package com.example;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

// tag::declarations[]
@EntityType("counter")                                     // <1>
public class CounterEntity extends ValueEntity<Integer> {  // <2>

  @Override
  public Integer emptyState() { return 0; }                  // <4>
  // end::declarations[]

  @EntityKey("counter_id")                                   // <4>
  @PostMapping("/counter/{counter_id}/increase")             // <5>
  public Effect<Number> increaseBy(@RequestBody Number increaseBy) {
    int newCounter = currentState() + increaseBy.value();    // <6>
    return effects()
        .updateState(newCounter)                             // <7>
        .thenReply(new Number(newCounter));
  }

  // tag::behaviour[]
  @PutMapping("/counter/{id}/set")                          // <1>
  public Effect<Number> set(@RequestBody Number number) {
    int newCounter = number.value();
    return effects()
        .updateState(newCounter)                            // <2>
        .thenReply(new Number(newCounter));                 // <3>
  }

  @PostMapping("/counter/{id}/plusone")                     // <4>
  public Effect<Number> plusOne() {
    int newCounter = currentState() + 1;                    // <5>
    return effects()
        .updateState(newCounter)                            // <6>
        .thenReply(new Number(newCounter));
  }
  // end::behaviour[]

  // tag::query[]
  @GetMapping("/counter/{id}")                   // <1>
  public Effect<Number> get() {
    return effects()
        .reply(new Number(currentState()));      // <2>
  }
  // end::query[]
}
