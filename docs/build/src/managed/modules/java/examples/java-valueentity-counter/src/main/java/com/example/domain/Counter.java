/* This code was initialised by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.domain;

import io.grpc.Status;
import kalix.javasdk.Metadata;
import kalix.javasdk.valueentity.ValueEntityContext;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import java.util.Optional;


// tag::class[]
/**
 * A Counter represented as a value entity.
 */
public class Counter extends AbstractCounter { // <1>

  // end::class[]
  @SuppressWarnings("unused")
  // tag::class[]
  private final String entityId;

  public Counter(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public CounterDomain.CounterState emptyState() { // <2>
    return CounterDomain.CounterState.getDefaultInstance();
  }
  // end::class[]

  // tag::increase[]
  @Override
  public Effect<Empty> increase(
      CounterDomain.CounterState currentState, CounterApi.IncreaseValue command) {
    if (command.getValue() < 0) { // <1>
      return effects().error("Increase requires a positive value. It was [" +
          command.getValue() + "].");
    }
    CounterDomain.CounterState newState =  // <2>
        currentState.toBuilder().setValue(currentState.getValue() +
            command.getValue()).build();
    return effects()
        .updateState(newState) // <3>
        .thenReply(Empty.getDefaultInstance());  // <4>
  }
  // end::increase[]

  @Override
  public Effect<Empty> increaseWithConditional(
      CounterDomain.CounterState currentState, CounterApi.IncreaseValue command) {
    if (command.getValue() < 0) {
      return effects().error("Increase requires a positive value. It was [" +
          command.getValue() + "].", Status.Code.INVALID_ARGUMENT);
    }
    CounterDomain.CounterState newState;
    if (commandContext().metadata().get("myKey").equals(Optional.of("myValue"))) {
      newState = currentState.toBuilder().setValue(currentState.getValue() +
          command.getValue() * 2).build();
    } else {
      newState = currentState.toBuilder().setValue(currentState.getValue() +
          command.getValue()).build();
    }
    return effects()
        .updateState(newState)
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> decrease(
      CounterDomain.CounterState currentState,
      CounterApi.DecreaseValue command) {
    if (command.getValue() < 0) {
      return effects().error("Decrease requires a positive value. It was [" +
          command.getValue() + "].");
    }
    CounterDomain.CounterState newState =
        currentState.toBuilder().setValue(currentState.getValue() -
            command.getValue()).build();
    return effects()
        .updateState(newState)
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> reset(
      CounterDomain.CounterState currentState, CounterApi.ResetValue command) {
    CounterDomain.CounterState newState =
        currentState.toBuilder().setValue(0).build();
    return effects()
        .updateState(newState)
        .thenReply(Empty.getDefaultInstance());
  }

  // tag::getCurrentCounter[]
  @Override
  public Effect<CounterApi.CurrentCounter> getCurrentCounter(
      CounterDomain.CounterState currentState, // <1>
      CounterApi.GetCounter command) {
    CounterApi.CurrentCounter current =
        CounterApi.CurrentCounter.newBuilder()
            .setValue(currentState.getValue()).build(); // <2>
    return effects().reply(current);
  }
  // end::getCurrentCounter[]

  // tag::delete[]
  @Override
  public Effect<Empty> delete(CounterDomain.CounterState currentState,
                              CounterApi.DeleteCounter deleteCounter) {
    return effects()
        .deleteEntity() // <1>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::delete[]
}
