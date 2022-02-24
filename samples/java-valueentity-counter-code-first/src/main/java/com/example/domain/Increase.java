package com.example.domain;

import kalix.javasdk.EntityId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Increase {

  @EntityId
  final public String counterId;
  final public long increaseBy;

  @JsonCreator
  public Increase(@JsonProperty("counterId") String counterId, @JsonProperty("increaseBy") long increaseBy) {
    this.counterId = counterId;
    this.increaseBy = increaseBy;
  }
}
