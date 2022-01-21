package com.example.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ValueDecreased {
  public final int value;

  @JsonCreator
  public ValueDecreased(int value) {
    this.value = value;
  }
  
  public static ValueDecreased of(int value) {
    return new ValueDecreased(value);
  }
}