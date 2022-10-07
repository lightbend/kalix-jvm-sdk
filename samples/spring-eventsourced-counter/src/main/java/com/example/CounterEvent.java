package com.example;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.example.CounterEvent.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = ValueIncreased.class, name = "value-increased"),
        @JsonSubTypes.Type(value = ValueMultiplied.class, name = "value-multiplied"),
    })
public sealed interface CounterEvent {

  record ValueIncreased(int value) implements CounterEvent {
  }

  record ValueMultiplied(int value) implements CounterEvent {
  }
}
