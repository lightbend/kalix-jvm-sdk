package org.example.valueentity;

import com.google.protobuf.Empty;
import kalix.javasdk.valueentity.ValueEntityContext;
import org.example.valueentity.domain.CounterDomain;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Value Entity Service described in your counter_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterServiceEntity extends AbstractCounterServiceEntity {
  @SuppressWarnings("unused")
  private final String entityId;

  public CounterServiceEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public CounterDomain.CounterState emptyState() {
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
  }

  @Override
  public Effect<Empty> increase(CounterDomain.CounterState currentState, CounterApi.IncreaseValue increaseValue) {
    return effects().error("The command handler for `Increase` is not implemented, yet");
  }

  @Override
  public Effect<Empty> decrease(CounterDomain.CounterState currentState, CounterApi.DecreaseValue decreaseValue) {
    return effects().error("The command handler for `Decrease` is not implemented, yet");
  }
}
