package com.example.wiring.eventsourcedentities.tracingcounter;

import kalix.javasdk.annotations.TypeName;

sealed public interface TCounterEvent {


    @TypeName("tincreased")
    record ValueIncreased(Integer value) implements TCounterEvent{}

    ;
}
