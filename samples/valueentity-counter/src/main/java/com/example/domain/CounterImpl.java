package com.example.domain;

import com.akkaserverless.javasdk.EntityId;
import com.example.CounterApi;
import com.google.protobuf.Empty;

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

    @Override
    protected CounterDomain.CounterState emptyState() {
        return CounterDomain.CounterState.getDefaultInstance();
    }

    // tag::increase[]
    @Override
    public Effect<Empty> increase(
            CounterDomain.CounterState currentState,
            CounterApi.IncreaseValue command) {
        if (command.getValue() < 0) { // <1>
            return effects().error("Increase requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState newState =  // <4>
                currentState.toBuilder().setValue(currentState.getValue() + command.getValue()).build();
        return effects()
                .updateState(newState) // <5>
                .thenReply(Empty.getDefaultInstance()); // FIXME add convenience shortcut for reply Empty?
    }
// end::increase[]

    @Override
    public Effect<Empty> decrease(
            CounterDomain.CounterState currentState,
            CounterApi.DecreaseValue command) {
        if (command.getValue() < 0) {
            return effects().error("Decrease requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState newState =
                currentState.toBuilder().setValue(currentState.getValue() - command.getValue()).build();
        return effects()
                .updateState(newState)
                .thenReply(Empty.getDefaultInstance());
    }

    @Override
    public Effect<Empty> reset(
            CounterDomain.CounterState currentState,
            CounterApi.ResetValue command) {
        CounterDomain.CounterState newState =
                currentState.toBuilder().setValue(0).build();
        return effects()
                .updateState(newState)
                .thenReply(Empty.getDefaultInstance());
    }

    // tag::getCurrentCounter[]
    @Override
    public Effect<CounterApi.CurrentCounter> getCurrentCounter(
            CounterDomain.CounterState currentState,
            CounterApi.GetCounter command) {
        CounterApi.CurrentCounter current =
                CounterApi.CurrentCounter.newBuilder().setValue(currentState.getValue()).build();
        return effects().reply(current);
    }

    // end::getCurrentCounter[]
}
