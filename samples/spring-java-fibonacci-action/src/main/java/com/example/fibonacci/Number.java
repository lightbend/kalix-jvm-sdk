package com.example.fibonacci;
 
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Number {

  public final long value;

  @JsonCreator
  public Number(@JsonProperty("value") long value) {
    this.value = value;
  }
}
