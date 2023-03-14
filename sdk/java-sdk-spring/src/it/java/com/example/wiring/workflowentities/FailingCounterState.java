package com.example.wiring.workflowentities;

public record FailingCounterState(String counterId, int value, boolean finished) {
  public FailingCounterState asFinished() {
    return new FailingCounterState(counterId, value, true);
  }

  public FailingCounterState inc() {
    return new FailingCounterState(counterId, value + 1, finished);
  }
}
