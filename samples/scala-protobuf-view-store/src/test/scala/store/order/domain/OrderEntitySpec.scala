package store.order.domain

import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import store.order.api

class OrderEntitySpec extends AnyWordSpec with Matchers {

  "OrderValueEntity" must {

    "handle Create and Get commands" in {
      val service = OrderEntityTestKit(new OrderEntity(_))
      val order = api.Order(orderId = "O1234", productId = "P123", customerId = "C001", quantity = 42)
      val createResult = service.create(order)
      createResult.reply shouldBe Empty.defaultInstance
      service.currentState() should be(
        OrderState(
          orderId = "O1234",
          productId = "P123",
          customerId = "C001",
          quantity = 42,
          created = service.currentState().created))
      val getResult = service.get(api.GetOrder("O1234"))
      getResult.reply shouldBe order
    }

  }
}
