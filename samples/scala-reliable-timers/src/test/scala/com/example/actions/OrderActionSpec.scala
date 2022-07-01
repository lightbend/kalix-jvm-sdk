package com.example.actions

import scala.concurrent.Future

import com.example.OrderService
import com.google.protobuf.empty.Empty
import kalix.scalasdk.testkit.MockRegistry
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class OrderActionSpec extends AsyncWordSpec with Matchers with ScalaFutures with AsyncMockFactory {

  "OrderAction" must {

    "handle command PlaceOrder" in {

      val orderService = mock[OrderService]
      (orderService.placeOrder _)
        .expects(*)
        .returning(Future.successful(Empty()))

      val mockRegistry = MockRegistry.withMock(orderService)

      val service = OrderActionTestKit(new OrderAction(_), mockRegistry)
      val result =
        service.placeOrder(OrderRequest(item = "Pizza Margherita", quantity = 3))

      val timer = result.nextSingleTimerDetails
      timer.name should startWith("order-expiration-timer")
      val deferredCall = timer.deferredCallDetails[OrderRequest, Empty]

      deferredCall.serviceName shouldBe "com.example.actions.Order"
      deferredCall.methodName shouldBe "Expire"

      val callResult = result.asyncResult.futureValue
      callResult.isError shouldBe false
      callResult.reply.number shouldNot be(null)
    }

  }
}
