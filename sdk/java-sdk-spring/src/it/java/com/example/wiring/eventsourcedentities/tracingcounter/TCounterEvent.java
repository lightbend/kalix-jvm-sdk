/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.eventsourcedentities.tracingcounter;

import kalix.javasdk.annotations.TypeName;

public interface TCounterEvent {

    @TypeName("tincreased")
    record ValueIncreased(Integer value) implements TCounterEvent{}
}
