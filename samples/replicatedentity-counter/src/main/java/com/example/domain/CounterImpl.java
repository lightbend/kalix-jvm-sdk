package com.example.domain;

import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.replicatedentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

// tag::class[]
/**
 * A Counter represented as a replicated entity.
 */
@ReplicatedEntity // <1>
public class CounterImpl extends CounterInterface {
    @SuppressWarnings("unused")
    private final String entityId;
    private final PNCounter counter;

    public CounterImpl(ReplicatedEntityCreationContext context) { // <2>
        this.entityId = context.entityId();
        this.counter = context.state(PNCounter.class).orElseGet(context::newPNCounter);
    }
    // end::class[]

    // tag::increase[]
    @Override
    public Reply<Empty> increase(CounterApi.IncreaseValue command, CommandContext ctx) {
        if (command.getValue() < 0) { // <1>
            throw ctx.fail("Increase requires a positive value. It was [" + command.getValue() + "].");
        }

        counter.increment(command.getValue());

        return Reply.message(Empty.getDefaultInstance());
    }
    // end::increase[]

    @Override
    public Reply<Empty> decrease(CounterApi.DecreaseValue command, CommandContext ctx) {
        if (command.getValue() < 0) {
            throw ctx.fail("Decrease requires a positive value. It was [" + command.getValue() + "].");
        }

        counter.decrement(command.getValue());

        return Reply.message(Empty.getDefaultInstance());
    }

    // tag::getCurrentCounter[]
    @Override
    public Reply<CounterApi.CurrentCounter> getCurrentCounter(CounterApi.GetCounter command, CommandContext ctx) {
        CounterApi.CurrentCounter current = CounterApi.CurrentCounter.newBuilder().setValue(counter.getValue()).build();
        return Reply.message(current);
    }
    // end::getCurrentCounter[]

// tag::class[]
}
// end::class[]
