/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.eventsourcedentities.counter;

import kalix.javasdk.annotations.TypeName;

public sealed interface CounterEvent {

  @TypeName("increased")
  record ValueIncreased(int value) implements CounterEvent {
  }

  @TypeName("set")
  record ValueSet(int value) implements CounterEvent {
  }

  @TypeName("multiplied")
  record ValueMultiplied(int value) implements CounterEvent {
  }
}
