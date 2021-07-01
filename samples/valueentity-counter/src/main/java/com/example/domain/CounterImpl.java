package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.EntityId;
import com.example.CounterApi;
import com.google.protobuf.Empty;

import java.util.Optional;

// tag::class[]
/**
 * A Counter represented as a value entity.
 */
public class CounterImpl extends CounterInterface2 { // <1>
    @SuppressWarnings("unused")
    private final String entityId;

    public CounterImpl(@EntityId String entityId) { // <2>
        this.entityId = entityId;
    }
// end::class[]

    // tag::increase[]
    @Override
    public Effect<Empty> increase(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.IncreaseValue command) {
        if (command.getValue() < 0) { // <1>
            return effects().failure("Increase requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = currentState // <2>
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build()); // <3>
        CounterDomain.CounterState newState =  // <4>
                state.toBuilder().setValue(state.getValue() + command.getValue()).build();
        return effects()
                .updateState(newState) // <5>
                .thenReply(Empty.getDefaultInstance()); // FIXME add convenience shortcut for reply Empty?
    }
// end::increase[]

    @Override
    public Effect<Empty> decrease(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.DecreaseValue command) {
        if (command.getValue() < 0) {
            return effects().failure("Decrease requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState state = currentState
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        CounterDomain.CounterState newState =
                state.toBuilder().setValue(state.getValue() - command.getValue()).build();
        return effects()
                .updateState(newState)
                .thenReply(Empty.getDefaultInstance());
    }

    @Override
    public Effect<Empty> reset(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.ResetValue command) {
        CounterDomain.CounterState state = currentState
                .orElseGet(() -> CounterDomain.CounterState.newBuilder().build());
        CounterDomain.CounterState newState =
                state.toBuilder().setValue(0).build();
        return effects()
                .updateState(newState)
                .thenReply(Empty.getDefaultInstance());
    }

    // tag::getCurrentCounter[]
    @Override
    public Effect<CounterApi.CurrentCounter> getCurrentCounter(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.GetCounter command) {
        CounterApi.CurrentCounter current = currentState // <1>
                .map((state) -> CounterApi.CurrentCounter.newBuilder().setValue(state.getValue()).build()) // <2>
                .orElseGet(() -> CounterApi.CurrentCounter.newBuilder().setValue(0).build()); // <3>
        return effects().reply(current);
    }
    // end::getCurrentCounter[]
}
