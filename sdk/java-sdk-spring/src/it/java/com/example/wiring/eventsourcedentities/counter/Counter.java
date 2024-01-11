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

package com.example.wiring.eventsourcedentities.counter;

public record Counter(Integer value) {

  public Counter onValueIncreased(CounterEvent.ValueIncreased evt) {
    return new Counter(this.value + evt.value());
  }

  public Counter onValueSet(CounterEvent.ValueSet evt) {
    return new Counter(evt.value());
  }

  public Counter onValueMultiplied(CounterEvent.ValueMultiplied evt) {
    return new Counter(this.value * evt.value());
  }

  public Counter apply(CounterEvent counterEvent) {
    if (counterEvent instanceof CounterEvent.ValueIncreased increased) {
      return onValueIncreased(increased);
    } else if (counterEvent instanceof CounterEvent.ValueSet set) {
      return onValueSet(set);
    } else if (counterEvent instanceof CounterEvent.ValueMultiplied multiplied) {
      return onValueMultiplied(multiplied);
    } else {
      throw new RuntimeException("Unknown event type: " + counterEvent);
    }
  }
}
