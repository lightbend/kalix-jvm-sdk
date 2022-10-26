package com.example;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.Entity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Entity(entityKey = "id", entityType = "counter")          // <1>
public class CounterEntity extends ValueEntity<Integer> {  // <2>

  @Override
  public Integer emptyState() { return 0; }                   // <3>

  @PostMapping("/counter/{id}/increase")                   // <4>
  public Effect<Number> increaseBy(@RequestBody Number increaseBy) {
    int newCounter = currentState() + increaseBy.value();    // <5>
    return effects()
        .updateState(newCounter)                             // <6>
        .thenReply(new Number(newCounter));
  }
}
