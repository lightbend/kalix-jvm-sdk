/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
