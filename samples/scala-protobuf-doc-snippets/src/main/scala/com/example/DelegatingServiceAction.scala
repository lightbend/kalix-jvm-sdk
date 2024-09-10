package com.example

import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import com.google.protobuf.empty.Empty
import akka.grpc.scaladsl.SingleResponseRequestBuilder

import scala.concurrent.ExecutionContext

class DelegatingServiceAction(creationContext: ActionCreationContext) extends AbstractDelegatingServiceAction {

  // tag::delegating-action[]
  override def addAndReturn(request: Request): Action.Effect[Result] = {
    implicit val executionContext: ExecutionContext = ExecutionContext.global

    val counterService = actionContext.getGrpcClient(classOf[CounterService], "counter") // <1>

    val increaseValue = IncreaseValue(counterId = request.counterId, value = 1)
    val increaseCompleted = counterService.increase(increaseValue) // <2>

    val currentCounterValueAfter = increaseCompleted.flatMap(_ => // <3>
      // once increase completed successfully, ask for the current state after
      counterService.getCurrentCounter(GetCounter(counterId = request.counterId))
    )

    // turn the reply from the other service into our reply type
    val result = currentCounterValueAfter.map(currentCounterValueAfter => // <4>
      Result(currentCounterValueAfter.value))

    effects.asyncReply(result) // <5>
  }
  // end::delegating-action[]

  def addAndReturnLifted(request: Request): Action.Effect[Result] = {
    implicit val executionContext: ExecutionContext = ExecutionContext.global

    val counterService = actionContext.getGrpcClient(classOf[CounterService], "counter")
    // tag::delegating-action-lifted[]
    val counterServiceClientBuilder: SingleResponseRequestBuilder[IncreaseValue, Empty] =
      counterService.asInstanceOf[CounterServiceClient].increase().addHeader("key","value") // <1>
    val increaseValue = IncreaseValue(counterId = request.counterId, value = 1)
    val increaseCompleted = counterServiceClientBuilder.invoke(increaseValue) // <2>
    // end::delegating-action-lifted[]

    val currentCounterValueAfter = increaseCompleted.flatMap(_ =>
      // once increase completed successfully, ask for the current state after
      counterService.getCurrentCounter(GetCounter(counterId = request.counterId))
    )

    // turn the reply from the other service into our reply type
    val result = currentCounterValueAfter.map(currentCounterValueAfter =>
      Result(currentCounterValueAfter.value))

    effects.asyncReply(result)
  }
}
