package com.example;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@EntityType("counter")                                     // <1>
public class CounterEntity extends ValueEntity<Integer> {  // <2>

  @Override
  public Integer emptyState() { return 0; }                  // <3>

  @EntityKey("counter_id")                                   // <4>
  @PostMapping("/counter/{counter_id}/increase")             // <5>
  public Effect<Number> increaseBy(@RequestBody Number increaseBy) {
    int newCounter = currentState() + increaseBy.value();    // <6>
    return effects()
        .updateState(newCounter)                             // <7>
        .thenReply(new Number(newCounter));
  }
}
