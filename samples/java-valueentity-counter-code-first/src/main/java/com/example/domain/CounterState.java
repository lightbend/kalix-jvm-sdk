package com.example.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CounterState {

  final public long value;

  @JsonCreator
  public CounterState(@JsonProperty("value") long value) {
    this.value = value;
  }

  public CounterState increase(long value) {
    return new CounterState(this.value + value);
  }


  public CounterState decrease(long value) {
    return new CounterState(this.value - value);
  }
}
