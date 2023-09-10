package com.example.wiring.eventsourcedentities.tracingcounter;

public record TCounter(Integer count){

    public TCounter onValueIncrease(Integer increase){
        return new TCounter(this.count + increase);
    }

}
