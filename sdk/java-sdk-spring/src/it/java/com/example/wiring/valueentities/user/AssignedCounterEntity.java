/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.user;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@TypeId("assigned-counter")
@Id("counterId")
@RequestMapping("/assigned-counter")
public class AssignedCounterEntity extends ValueEntity<AssignedCounter> {

  @Override
  public AssignedCounter emptyState() {
    return new AssignedCounter(commandContext().entityId(), "");
  }

  @PostMapping("/{counterId}/assign/{assigneeId}")
  public ValueEntity.Effect<String> assign(@PathVariable String assigneeId) {
    AssignedCounter newState = currentState().assignTo(assigneeId);
    return effects().updateState(newState).thenReply("OK");
  }
}
