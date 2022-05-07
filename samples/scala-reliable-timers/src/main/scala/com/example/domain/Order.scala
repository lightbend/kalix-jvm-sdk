package com.example.domain

import com.example
import com.example.OrderRequest
import com.google.protobuf.empty.Empty
import io.grpc.Status
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::order[]
class Order(context: ValueEntityContext) extends AbstractOrder {

  override def emptyState: OrderState =
    OrderState.defaultInstance

  override def placeOrder(currentState: OrderState, orderRequest: OrderRequest): ValueEntity.Effect[Empty] = {
    val placedOrder =
      OrderState(
      orderRequest.orderNumber, 
      placed = true, // <1>
      item = orderRequest.item,
      quantity = orderRequest.quantity)

    effects.updateState(placedOrder).thenReply(Empty.defaultInstance)
  }

  override def confirm(currentState: OrderState, confirmRequest: example.ConfirmRequest): ValueEntity.Effect[Empty] =
    if (currentState.placed) // <2>
      effects
        .updateState(currentState.copy(confirmed = true))
        .thenReply(Empty.defaultInstance)
    else
      effects
        .error(
          s"No order found for '${confirmRequest.orderNumber}'", 
          Status.Code.NOT_FOUND) // <3>

  override def cancel(currentState: OrderState, cancelRequest: example.CancelRequest): ValueEntity.Effect[Empty] =
    if (!currentState.placed)
      effects
        .error(
          s"No order found for '${cancelRequest.orderNumber}'", 
          Status.Code.NOT_FOUND) // <4>
    else if (currentState.confirmed)
      effects
        .error(
          "Can not cancel an already confirmed order", 
          Status.Code.INVALID_ARGUMENT) // <5>
    else
      effects.updateState(emptyState)
        .thenReply(Empty.defaultInstance) // <6>

  // end::order[]
  override def getOrderStatus(
      currentState: OrderState,
      orderStatusRequest: example.OrderStatusRequest): ValueEntity.Effect[example.OrderStatus] = {

    if (currentState.placed) {
      val status = example.OrderStatus(
        confirmed = currentState.confirmed,
        item = currentState.item,
        quantity = currentState.quantity)

      effects.reply(status)
    } else {
      effects.error(
        s"No order found for '${orderStatusRequest.orderNumber}'", 
        Status.Code.NOT_FOUND)
    }
  }
// tag::order[]
}
// end::order[]
