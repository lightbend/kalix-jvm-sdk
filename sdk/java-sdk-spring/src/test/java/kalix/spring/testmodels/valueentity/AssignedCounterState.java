/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.valueentity;

public class AssignedCounterState {
  public final String counterId;
  public final String assigneeId;

  public AssignedCounterState(String counterId, String assigneeId) {
    this.counterId = counterId;
    this.assigneeId = assigneeId;
  }

  public AssignedCounterState assignTo(String assigneeId) {
    return new AssignedCounterState(counterId, assigneeId);
  }
}
