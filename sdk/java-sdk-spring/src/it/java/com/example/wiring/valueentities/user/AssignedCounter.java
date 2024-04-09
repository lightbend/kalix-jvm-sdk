/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.user;

public class AssignedCounter {
  public final String counterId;
  public final String assigneeId;

  public AssignedCounter(String counterId, String assigneeId) {
    this.counterId = counterId;
    this.assigneeId = assigneeId;
  }

  public AssignedCounter assignTo(String assigneeId) {
    return new AssignedCounter(counterId, assigneeId);
  }
}
