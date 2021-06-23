package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

import java.util.Optional;

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
    public Effect<Empty> increase(
            CounterApi.IncreaseValue command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx) {
        if (command.getValue() < 0) { // <1>
            return effectBuilder.failure("Increase requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = currentState // <2>
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build()); // <3>
        CounterDomain.CounterState newState =  // <4>
                state.toBuilder().setValue(state.getValue() + command.getValue()).build();
        return effectBuilder
                .updateState(newState) // <5>
                .thenReply(Empty.getDefaultInstance()); // FIXME add convenience shortcut for reply Empty?
    }
// end::increase[]

    @Override
    public Effect<Empty> decrease(
            CounterApi.DecreaseValue command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx) {
        if (command.getValue() < 0) {
            return effectBuilder.failure("Decrease requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = currentState
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        CounterDomain.CounterState newState =
                state.toBuilder().setValue(state.getValue() - command.getValue()).build();
        return effectBuilder
                .updateState(newState)
                .thenReply(Empty.getDefaultInstance());
    }

    @Override
    public Effect<Empty> reset(
            CounterApi.ResetValue command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx) {
        CounterDomain.CounterState state = currentState
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        CounterDomain.CounterState newState =
                state.toBuilder().setValue(0).build();
        return effectBuilder
                .updateState(newState)
                .thenReply(Empty.getDefaultInstance());
    }

    // tag::getCurrentCounter[]
    @Override
    public Effect<CounterApi.CurrentCounter> getCurrentCounter(
            CounterApi.GetCounter command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<CounterApi.CurrentCounter, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx) {
        CounterApi.CurrentCounter current = currentState // <1>
                .map((state) -> CounterApi.CurrentCounter.newBuilder().setValue(state.getValue()).build()) // <2>
                .orElseGet(() -> CounterApi.CurrentCounter.newBuilder().setValue(0).build()); // <3>
        return effectBuilder.message(current);
    }
    // end::getCurrentCounter[]
}
