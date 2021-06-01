package com.example.domain;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

/** A Counter represented as a value entity. */
@ValueEntity(entityType = "counter")
public class CounterImpl extends CounterInterface {
    @SuppressWarnings("unused")
    private final String entityId;

    public CounterImpl(@EntityId String entityId) {
        this.entityId = entityId;
    }
    
    @Override
    protected Empty increase(CounterApi.IncreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        if (command.getValue() < 0) {
            throw ctx.fail("Increase requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = ctx.getState()
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        ctx.updateState(state.toBuilder().setValue(state.getValue() + command.getValue()).build());
        return Empty.getDefaultInstance();
    }
    
    @Override
    protected Empty decrease(CounterApi.DecreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        if (command.getValue() < 0) {
            throw ctx.fail("Decrease requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = ctx.getState()
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        ctx.updateState(state.toBuilder().setValue(state.getValue() - command.getValue()).build());
        return Empty.getDefaultInstance();
    }
    
    @Override
    protected Empty reset(CounterApi.ResetValue command, CommandContext<CounterDomain.CounterState> ctx) {
        CounterDomain.CounterState state = ctx.getState()
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        ctx.updateState(state.toBuilder().setValue(0).build());
        return Empty.getDefaultInstance();
    }
    
    @Override
    protected CounterApi.CurrentCounter getCurrentCounter(CounterApi.GetCounter command, CommandContext<CounterDomain.CounterState> ctx) {
        CounterDomain.CounterState state = ctx.getState()
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        return CounterApi.CurrentCounter.newBuilder().setValue(state.getValue()).build();
    }
}
