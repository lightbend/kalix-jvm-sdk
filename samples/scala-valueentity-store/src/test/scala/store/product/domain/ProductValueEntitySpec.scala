package store.product.domain

import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import store.product.api

class ProductValueEntitySpec extends AnyWordSpec with Matchers {

  "ProductValueEntity" must {

    "handle Create and Get commands" in {
      val service = ProductValueEntityTestKit(new ProductValueEntity(_))
      val product = api.Product(
        productId = "P123",
        productName = "Super Duper Thingamajig",
        price = Some(api.Money("USD", 123, 45)))
      val createResult = service.create(product)
      createResult.reply shouldBe Empty.defaultInstance
      service.currentState() shouldBe ProductState(
        productId = "P123",
        productName = "Super Duper Thingamajig",
        price = Some(Money("USD", 123, 45)))
      val getResult = service.get(api.GetProduct("P123"))
      getResult.reply shouldBe product
    }

  }
}
