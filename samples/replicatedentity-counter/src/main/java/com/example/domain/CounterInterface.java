package com.example.domain;

import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.replicatedentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

// FIXME codegen of ReplicatedEntity

/** A replicated entity. */
public abstract class CounterInterface {

    @CommandHandler(name = "Increase")
    public abstract Reply<Empty> increase(CounterApi.IncreaseValue command, CommandContext ctx);

    @CommandHandler(name = "Decrease")
    public abstract Reply<Empty> decrease(CounterApi.DecreaseValue command, CommandContext ctx);

    @CommandHandler(name = "GetCurrentCounter")
    public abstract Reply<CounterApi.CurrentCounter> getCurrentCounter(CounterApi.GetCounter command, CommandContext ctx);

}
