package com.example.domain;

import kalix.javasdk.EntityId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Reset {

  @EntityId
  final public String counterId;

  @JsonCreator
  public Reset(@JsonProperty("counterId") String counterId) {
    this.counterId = counterId;
  }
}
