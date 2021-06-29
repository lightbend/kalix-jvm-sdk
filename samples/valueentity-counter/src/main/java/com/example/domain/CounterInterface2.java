package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntityEffect;
import com.example.CounterApi;
import com.google.protobuf.Empty;

import java.util.Optional;

/** A value entity. */
public abstract class CounterInterface2 {

    @CommandHandler(name = "Increase")
    public abstract Effect<Empty> increase(
            CounterApi.IncreaseValue command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx);

    @CommandHandler(name = "Decrease")
    public abstract Effect<Empty> decrease(
            CounterApi.DecreaseValue command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx);

    @CommandHandler(name = "Reset")
    public abstract Effect<Empty> reset(
            CounterApi.ResetValue command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx);

    @CommandHandler(name = "GetCurrentCounter")
    public abstract Effect<CounterApi.CurrentCounter> getCurrentCounter(
            CounterApi.GetCounter command,
            Optional<CounterDomain.CounterState> currentState,
            ValueEntityEffect.Builder<CounterApi.CurrentCounter, CounterDomain.CounterState> effectBuilder,
            CommandContext<CounterDomain.CounterState> ctx);

}
