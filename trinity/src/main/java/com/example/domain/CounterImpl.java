package com.example.domain;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

/** A value entity. */
@ValueEntity(entityType = "counter")
public class CounterImpl extends CounterInterface {
    @SuppressWarnings("unused")
    private final String entityId;
    
    public CounterImpl(@EntityId String entityId) {
        this.entityId = entityId;
    }
    
    @Override
    protected Empty increase(CounterApi.IncreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return Empty.getDefaultInstance();
    }

    
    @Override
    protected Empty decrease(CounterApi.DecreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return Empty.getDefaultInstance();
    }
    
    @Override
    protected Empty reset(CounterApi.ResetValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return Empty.getDefaultInstance();
    }
    
    @Override
    protected CounterApi.CurrentCounter getCurrentCounter(CounterApi.GetCounter command, CommandContext<CounterDomain.CounterState> ctx) {
        return CounterApi.CurrentCounter.newBuilder().build();
    }
}