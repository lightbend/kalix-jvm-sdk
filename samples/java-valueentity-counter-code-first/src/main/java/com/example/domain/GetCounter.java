package com.example.domain;


import kalix.javasdk.EntityId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetCounter {

  @EntityId
  final public String counterId;

  @JsonCreator
  public GetCounter(@JsonProperty("counterId") String counterId) {
    this.counterId = counterId;
  }

}
