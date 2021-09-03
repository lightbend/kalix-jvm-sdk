/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.domain;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.valueentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

/** A value entity. */
@ValueEntity(entityType = "counter")
public class Counter extends AbstractCounter {
    @SuppressWarnings("unused")
    private final String entityId;
    
    public Counter(@EntityId String entityId) {
        this.entityId = entityId;
    }
    
    @Override
    public Reply<Empty> increase(CounterApi.IncreaseValue command, CommandContext<CounterDomain.CounterState> context) {
        return Reply.failure("The command handler for `Increase` is not implemented, yet");
    }
    
    @Override
    public Reply<Empty> decrease(CounterApi.DecreaseValue command, CommandContext<CounterDomain.CounterState> context) {
        return Reply.failure("The command handler for `Decrease` is not implemented, yet");
    }
    
    @Override
    public Reply<Empty> reset(CounterApi.ResetValue command, CommandContext<CounterDomain.CounterState> context) {
        return Reply.failure("The command handler for `Reset` is not implemented, yet");
    }
    
    @Override
    public Reply<CounterApi.CurrentCounter> getCurrentCounter(CounterApi.GetCounter command, CommandContext<CounterDomain.CounterState> context) {
        return Reply.failure("The command handler for `GetCurrentCounter` is not implemented, yet");
    }
}