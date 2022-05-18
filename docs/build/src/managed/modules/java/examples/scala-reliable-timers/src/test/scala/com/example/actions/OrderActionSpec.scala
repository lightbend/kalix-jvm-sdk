package com.example.actions

import com.example.OrderStatus
import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class OrderActionSpec
    extends AnyWordSpec
    with Matchers {

  "OrderAction" must {

    "have example test that can be removed" in {
      val service = OrderActionTestKit(new OrderAction(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // result.reply shouldBe expectedReply
    }

    "handle command PlaceOrder" in {
      val service = OrderActionTestKit(new OrderAction(_))
          pending
      // val result = service.placeOrder(OrderRequest(...))
    }

    "handle command Confirm" in {
      val service = OrderActionTestKit(new OrderAction(_))
          pending
      // val result = service.confirm(OrderNumber(...))
    }

    "handle command Cancel" in {
      val service = OrderActionTestKit(new OrderAction(_))
          pending
      // val result = service.cancel(OrderNumber(...))
    }

    "handle command Expire" in {
      val service = OrderActionTestKit(new OrderAction(_))
          pending
      // val result = service.expire(OrderNumber(...))
    }

    "handle command GetOrderStatus" in {
      val service = OrderActionTestKit(new OrderAction(_))
          pending
      // val result = service.getOrderStatus(OrderNumber(...))
    }

  }
}
