package com.example.actions

import com.example.{CounterService, IncreaseValue}
import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ExternalCounterAction(creationContext: ActionCreationContext) extends AbstractExternalCounterAction {

  override def increase(increaseValue: IncreaseValue): Action.Effect[Empty] = {
    val counterService = actionContext.getGrpcClient(classOf[CounterService], "counter")
    effects.asyncReply(counterService.increase(IncreaseValue()))
  }
}

