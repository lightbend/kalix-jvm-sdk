package com.example.actions;

import com.example.CounterApi;
import com.example.CounterService;
import com.google.protobuf.Empty;
import kalix.javasdk.action.ActionCreationContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/actions/external_counter.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class ExternalCounterAction extends AbstractExternalCounterAction {

  public ExternalCounterAction(ActionCreationContext creationContext) {}

  @Override
  public Effect<Empty> increase(CounterApi.IncreaseValue increaseValue) {
    var counterService = actionContext().getGrpcClient(CounterService.class, "counter");
    return effects().asyncReply(counterService.increase(CounterApi.IncreaseValue.newBuilder().build()));
  }
}
