/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
