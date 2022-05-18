package com.example.domain

import com.example
import com.google.protobuf.empty.Empty
import kalix.scalasdk.testkit.ValueEntityResult
import kalix.scalasdk.valueentity.ValueEntity
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OrderSpec
    extends AnyWordSpec
    with Matchers {

  "Order" must {

    "have example test that can be removed" in {
      val service = OrderTestKit(new Order(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // val reply = result.getReply()
      // reply shouldBe expectedReply
      // verify the final state after the command
      // service.currentState() shouldBe expectedState
    }

    "handle command PlaceOrder" in {
      val service = OrderTestKit(new Order(_))
      pending
      // val result = service.placeOrder(example.OrderRequest(...))
    }

    "handle command Confirm" in {
      val service = OrderTestKit(new Order(_))
      pending
      // val result = service.confirm(example.ConfirmRequest(...))
    }

    "handle command Cancel" in {
      val service = OrderTestKit(new Order(_))
      pending
      // val result = service.cancel(example.CancelRequest(...))
    }

    "handle command GetOrderStatus" in {
      val service = OrderTestKit(new Order(_))
      pending
      // val result = service.getOrderStatus(example.OrderStatusRequest(...))
    }

  }
}
