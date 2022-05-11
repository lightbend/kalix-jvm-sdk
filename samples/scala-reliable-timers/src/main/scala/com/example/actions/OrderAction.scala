package com.example.actions

import java.util.UUID

import com.example.domain.Order
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.Done
import com.example
import com.example.OrderStatus
import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::timers[]
class OrderAction(creationContext: ActionCreationContext) extends AbstractOrderAction {
// end::timers[]
  val logger = LoggerFactory.getLogger(classOf[Order])

  // tag::place-order[]
  def timerName(orderNum: OrderNumber) =
    "order-expiration-timer-" + orderNum.number

  override def placeOrder(orderRequest: OrderRequest): Action.Effect[OrderNumber] = {

    val orderNumber = OrderNumber(UUID.randomUUID().toString) // <1>

    val timerRegistration: Future[Done] = // <2>
      timers.startSingleTimer(
        name = timerName(orderNumber), // <3>
        delay = 5.minutes, // <4>
        deferredCall = components.orderAction.expire(orderNumber) // <5>
      )

    def placeOrder(): Future[OrderNumber] = // <6>
      components.order
        .placeOrder(example
          .OrderRequest(orderNumber = orderNumber.number, item = orderRequest.item, quantity = orderRequest.quantity))
        .execute() // <7>
        .map(_ => orderNumber)

    // end::place-order[]
    logger.info(
      "Placing order for item {} (quantity {}). Order number '{}'",
      orderRequest.item,
      orderRequest.quantity,
      orderNumber.number)
    // tag::place-order[]
    effects.asyncReply(timerRegistration.flatMap(_ => placeOrder())) // <8>
  }
  // end::place-order[]

  // tag::expire-order[]
  override def expire(orderNumber: OrderNumber): Action.Effect[Empty] = {
    logger.info("Expiring order '{}'", orderNumber.number)

    def validateErrorCodes(code: Status.Code) =
      code == Status.Code.NOT_FOUND || code == Status.Code.INVALID_ARGUMENT

    val result =
      components.order
        .cancel(example.CancelRequest(orderNumber.number))
        .execute() // <1>
        .map { _ => Empty.defaultInstance } // <2>
        .recover { // <3>
          case ex: StatusRuntimeException if validateErrorCodes(ex.getStatus.getCode) =>
            // if NotFound or InvalidArgument, we don't need to re-try, and we can move on
            // other kind of failures are not recovered and will trigger a re-try
            Empty.defaultInstance
        }

    effects.asyncReply(result)
  }
  // end::expire-order[]

  // tag::confirm-cancel-order[]
  override def confirm(orderNumber: OrderNumber): Action.Effect[Empty] = {
    logger.info("Confirming order '{}'", orderNumber.number)
    val reply =
      for {
        _ <- components.order // <1>
          .confirm(example.ConfirmRequest(orderNumber.number))
          .execute()
        _ <- timers.cancel(timerName(orderNumber)) // <2>
      } yield Empty.defaultInstance

    effects.asyncReply(reply)
  }

  override def cancel(orderNumber: OrderNumber): Action.Effect[Empty] = {
    logger.info("Cancelling order '{}'", orderNumber.number)
    val reply =
      for {
        _ <- components.order
          .cancel(example.CancelRequest(orderNumber.number))
          .execute()
        _ <- timers.cancel(timerName(orderNumber))
      } yield Empty.defaultInstance

    effects.asyncReply(reply)
  }
  // end::confirm-cancel-order[]

  override def getOrderStatus(orderNumber: OrderNumber): Action.Effect[OrderStatus] =
    effects.forward(components.order.getOrderStatus(example.OrderStatusRequest(orderNumber.number)))

// tag::timers[]

}
// end::timers[]
