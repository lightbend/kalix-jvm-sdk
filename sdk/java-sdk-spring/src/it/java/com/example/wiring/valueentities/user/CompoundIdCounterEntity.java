/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.user;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.valueentity.ValueEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@TypeId("compound-id-counter")
@Id({"id_part_1", "id_part_2"})
@RequestMapping("/compound-id-counter")
public class CompoundIdCounterEntity extends ValueEntity<Integer> {


  @Override
  public Integer emptyState() {
    return 0;
  }

  @PostMapping("/{id_part_1}/{id_part_2}/set/{value}")
  public Effect<String> set(@PathVariable Integer value) {
    return effects().updateState(value).thenReply("OK");
  }

  @GetMapping("/{id_part_1}/{id_part_2}")
  public Effect<Integer> get() {
    return effects().reply(currentState());
  }
}
