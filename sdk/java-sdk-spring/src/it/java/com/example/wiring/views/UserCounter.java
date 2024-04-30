/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.views;

import com.example.wiring.eventsourcedentities.counter.CounterEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class UserCounter {

  public final String id;
  public final Integer value;

  @JsonCreator
  public UserCounter(@JsonProperty("id") String id, @JsonProperty("value") Integer value) {
    this.id = id;
    this.value = value;
  }

  public UserCounter onValueIncreased(CounterEvent.ValueIncreased event) {
    return new UserCounter(id, value + event.value());
  }

  public UserCounter onValueMultiplied(CounterEvent.ValueMultiplied event) {
    return new UserCounter(id, value * event.value());
  }


  public UserCounter onValueSet(CounterEvent.ValueSet event) {
    return new UserCounter(id, event.value());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    UserCounter that = (UserCounter) o;
    return Objects.equals(id, that.id) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, value);
  }
}
