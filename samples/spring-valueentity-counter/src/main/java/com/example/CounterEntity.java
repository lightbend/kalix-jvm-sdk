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
  public Integer emptyState() { return 0; }                  // <3>
  // end::declarations[]

  // tag::increase[]

  @EntityKey("counter_id")                                   // <4>
  @PostMapping("/counter/{counter_id}/increase")             // <5>
  public Effect<Number> increaseBy(@RequestBody Number increaseBy) {
    int newCounter = currentState() + increaseBy.value();    // <6>
    return effects()
        .updateState(newCounter)                             // <7>
        .thenReply(new Number(newCounter));
  }
  // end::increase[]

  // tag::behaviour[]
  @EntityKey("counter_id")
  @PutMapping("/counter/{counter_id}/set")                  // <1>
  public Effect<Number> set(@RequestBody Number number) {
    int newCounter = number.value();
    return effects()
        .updateState(newCounter)                            // <2>
        .thenReply(new Number(newCounter));                 // <3>
  }

  @EntityKey("counter_id")
  @PostMapping("/counter/{counter_id}/plusone")             // <4>
  public Effect<Number> plusOne() {
    int newCounter = currentState() + 1;                    // <5>
    return effects()
        .updateState(newCounter)                            // <6>
        .thenReply(new Number(newCounter));
  }
  // end::behaviour[]

  // tag::query[]
  @EntityKey("counter_id")
  @GetMapping("/counter/{counter_id}")           // <1>
  public Effect<Number> get() {
    return effects()
        .reply(new Number(currentState()));      // <2>
  }
  // end::query[]
  // tag::close[]

}
// end::close[]