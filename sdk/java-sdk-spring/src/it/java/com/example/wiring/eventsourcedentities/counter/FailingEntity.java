package com.example.wiring.eventsourcedentities.counter;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.valueentity.ValueEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@TypeId("failing-entity")
@Id("id")
@RequestMapping("/failing-entity/{id}")
public class FailingEntity extends ValueEntity<Integer> {

  @PutMapping
  public Effect<String> doSth() {
    return effects().error("asd");
  }
}
