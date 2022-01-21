package com.example.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ValueIncreased {
  public final int value;

  @JsonCreator
  public ValueIncreased(int value) {
    this.value = value;
  }

  public static ValueIncreased of(int value) {
    return new ValueIncreased(value);
  }
}
