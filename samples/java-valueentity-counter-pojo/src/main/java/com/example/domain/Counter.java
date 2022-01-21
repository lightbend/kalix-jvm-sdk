/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.domain;

import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.example.CounterApi;
import com.google.protobuf.Empty;

/**
 * A Counter represented as a value entity.
 */
public class Counter extends AbstractCounter {

    @SuppressWarnings("unused")
    private final String entityId;

    public Counter(ValueEntityContext context) {
        this.entityId = context.entityId();
    }

    @Override
    public CounterState emptyState() {
      return new CounterState(0);
    }

    @Override
    public Effect<Empty> increase(
        CounterState currentState, CounterApi.IncreaseValue command) {
      return effects()
              .updateState(currentState.increase(command.getValue()))
              .thenReply(Empty.getDefaultInstance());
    }

    @Override
    public Effect<Empty> decrease(
            CounterState currentState,
            CounterApi.DecreaseValue command) {

      return effects()
          .updateState(currentState.decrease(command.getValue()))
          .thenReply(Empty.getDefaultInstance());
    }
    
    @Override
    public Effect<Empty> reset(
        CounterState currentState, CounterApi.ResetValue command) {
      return effects()
          .updateState(emptyState())
          .thenReply(Empty.getDefaultInstance());
    }
    
    @Override
    public Effect<CounterApi.CurrentCounter> getCurrentCounter(
            CounterState currentState,
            CounterApi.GetCounter command) {
        CounterApi.CurrentCounter current =
                CounterApi.CurrentCounter.newBuilder()
                    .setValue(currentState.value).build();
        return effects().reply(current);
    }
}
