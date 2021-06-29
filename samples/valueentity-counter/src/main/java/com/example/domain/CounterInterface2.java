package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.example.CounterApi;
import com.google.protobuf.Empty;

import java.util.Optional;

/** A value entity. */
public abstract class CounterInterface2 {

    @CommandHandler(name = "Increase")
    public abstract Effect<Empty> increase(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.IncreaseValue command,
            CommandContext<Empty, CounterDomain.CounterState> context);

    @CommandHandler(name = "Decrease")
    public abstract Effect<Empty> decrease(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.DecreaseValue command,
            CommandContext<Empty, CounterDomain.CounterState> context);

    @CommandHandler(name = "Reset")
    public abstract Effect<Empty> reset(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.ResetValue command,
            CommandContext<Empty, CounterDomain.CounterState> context);

    @CommandHandler(name = "GetCurrentCounter")
    public abstract Effect<CounterApi.CurrentCounter> getCurrentCounter(
            Optional<CounterDomain.CounterState> currentState,
            CounterApi.GetCounter command,
            CommandContext<CounterApi.CurrentCounter, CounterDomain.CounterState> context);

}
