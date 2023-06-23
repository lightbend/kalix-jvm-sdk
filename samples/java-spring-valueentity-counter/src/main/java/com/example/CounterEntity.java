package com.example;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;


import org.springframework.web.bind.annotation.*;

// tag::declarations[]
@TypeId("counter") // <1>
@Id("counter_id") // <2>
public class CounterEntity extends ValueEntity<Integer> { // <3>

  @Override
  public Integer emptyState() { return 0; } // <4>
  // end::declarations[]

  // tag::increase[]
  @PostMapping("/counter/{counter_id}/increase") // <5>
  public Effect<Number> increaseBy(@RequestBody Number increaseBy) {
    int newCounter = currentState() + increaseBy.value(); // <6>
    return effects()
        .updateState(newCounter) // <7>
        .thenReply(new Number(newCounter));
  }
  // end::increase[]

  // tag::behaviour[]
  @PutMapping("/counter/{counter_id}/set") // <1>
  public Effect<Number> set(@RequestBody Number number) {
    int newCounter = number.value();
    return effects()
        .updateState(newCounter) // <2>
        .thenReply(new Number(newCounter)); // <3>
  }

  @PostMapping("/counter/{counter_id}/plusone") // <4>
  public Effect<Number> plusOne() {
    int newCounter = currentState() + 1; // <5>
    return effects()
        .updateState(newCounter) // <6>
        .thenReply(new Number(newCounter));
  }
  // end::behaviour[]

  // tag::delete[]
  @DeleteMapping("/counter/{counter_id}")
  public Effect<String> delete() {
    return effects()
        .deleteEntity() // <1>
        .thenReply("deleted: " + commandContext().entityId());
  }
  // end::delete[]

  // tag::query[]
  @GetMapping("/counter/{counter_id}") // <1>
  public Effect<Number> get() {
    return effects()
        .reply(new Number(currentState())); // <2>
  }
  // end::query[]
  // tag::close[]

}
// end::close[]
