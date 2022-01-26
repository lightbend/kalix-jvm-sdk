package com.example.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public class CounterState {

  public final int value;

  @JsonCreator
  public CounterState(int value) {
    this.value = value;
  }

  public CounterState increase(int value){
    if (value <= 0) throw new IllegalArgumentException("increase value must be a positive number");
    return new CounterState(this.value + value);
  }

  public CounterState decrease(int value){
    if (value >= 0) throw new IllegalArgumentException("decrease value must be a negative number");
    return new CounterState(this.value + value);
  }

}
