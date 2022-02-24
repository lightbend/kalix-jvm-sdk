package com.example.domain;

import kalix.javasdk.EntityId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Decrease {

  @EntityId
  final public String counterId;
  final public long decreaseBy;

  @JsonCreator
  public Decrease(@JsonProperty("counterId") String counterId, @JsonProperty("decreaseBy") long decreaseBy) {
    this.counterId = counterId;
    this.decreaseBy = decreaseBy;
  }
}
