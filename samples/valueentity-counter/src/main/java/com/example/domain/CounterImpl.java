package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

// tag::class[]
/**
 * A Counter represented as a value entity.
 */
@ValueEntity(entityType = "counter") // <1>
public class CounterImpl extends CounterInterface2 {
    @SuppressWarnings("unused")
    private final String entityId;

    public CounterImpl(@EntityId String entityId) { // <2>
        this.entityId = entityId;
    }
// end::class[]

    // tag::increase[]
    @Override
    public Effect<Empty> increase(CounterApi.IncreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        if (command.getValue() < 0) { // <1>
            throw ctx.fail("Increase requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = ctx.getState() // <2>
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build()); // <3>
        CounterDomain.CounterState newState =  // <4>
                state.toBuilder().setValue(state.getValue() + command.getValue()).build();
        ctx.updateState(newState); // <5>
        return ValueEntityEffect.message(Empty.getDefaultInstance()); // FIXME add convenience shortcut for reply Empty?
    }
// end::increase[]

    @Override
    public Effect<Empty> decrease(CounterApi.DecreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        if (command.getValue() < 0) {
            throw ctx.fail("Decrease requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = ctx.getState()
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        ctx.updateState(state.toBuilder().setValue(state.getValue() - command.getValue()).build());
        return ValueEntityEffect.message(Empty.getDefaultInstance());
    }

    @Override
    public Effect<Empty> reset(CounterApi.ResetValue command, CommandContext<CounterDomain.CounterState> ctx) {
        CounterDomain.CounterState state = ctx.getState()
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        ctx.updateState(state.toBuilder().setValue(0).build());
        return ValueEntityEffect.message(Empty.getDefaultInstance());
    }

    // tag::getCurrentCounter[]
    @Override
    public Effect<CounterApi.CurrentCounter> getCurrentCounter(CounterApi.GetCounter command, CommandContext<CounterDomain.CounterState> ctx) {
        CounterApi.CurrentCounter current = ctx.getState() // <1>
                .map((state) -> CounterApi.CurrentCounter.newBuilder().setValue(state.getValue()).build()) // <2>
                .orElseGet(() -> CounterApi.CurrentCounter.newBuilder().setValue(0).build()); // <3>
        return ValueEntityEffect.message(current);
    }
    // end::getCurrentCounter[]
}
