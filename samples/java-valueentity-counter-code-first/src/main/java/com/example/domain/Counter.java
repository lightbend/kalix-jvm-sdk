/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.domain;

import kalix.javasdk.valueentity.ValueEntity;

public class Counter extends ValueEntity<CounterState> {

    @Override
    public CounterState emptyState() {
      return new CounterState(0);
    }
    public Effect<Response> increase(CounterState currentState, Increase command) {
      if (command.increaseBy < 0) {
        return effects().error("Increase requires a positive value. It was [" + command.increaseBy + "].");
      }
      return effects()
              .updateState(currentState.increase(command.increaseBy))
              .thenReply(Response.done());
    }

    public Effect<Response> decrease(CounterState currentState, Decrease command) {
      if (command.decreaseBy < 0) {
        return effects().error("Decrease requires a positive value. It was [" + command.decreaseBy + "].");
      }
      return effects()
          .updateState(currentState.decrease(command.decreaseBy))
          .thenReply(Response.done());
    }
    
    public Effect<Response> reset(CounterState currentState, Reset command) {
      return effects()
          .updateState(emptyState())
          .thenReply(Response.done());
    }
    
    public Effect<CurrentCounter> getCurrentCounter(CounterState currentState, GetCounter command) {
        CurrentCounter current = new CurrentCounter(currentState.value);
        return effects().reply(current);
    }
}
