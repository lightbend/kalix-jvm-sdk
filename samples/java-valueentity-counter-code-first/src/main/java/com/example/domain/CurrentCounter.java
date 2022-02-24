package com.example.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CurrentCounter {
  final public long value;

  @JsonCreator
  public CurrentCounter(@JsonProperty("value") long value) {
    this.value = value;
  }

}
