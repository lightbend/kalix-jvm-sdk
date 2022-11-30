package store.product.domain

import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import store.product.api

class ProductEntitySpec extends AnyWordSpec with Matchers {

  "ProductEntity" should {

    "handle Create and Get commands" in {
      val service = ProductEntityTestKit(new ProductEntity(_))

      val product = api.Product(
        productId = "P123",
        productName = "Super Duper Thingamajig",
        price = Some(api.Money("USD", 123, 45)))

      val productState =
        ProductState(productId = "P123", productName = "Super Duper Thingamajig", price = Some(Money("USD", 123, 45)))

      val createResult = service.create(product)
      createResult.reply shouldBe Empty.defaultInstance
      createResult.events.size shouldBe 1
      createResult.nextEvent[ProductCreated] shouldBe ProductCreated(product = Some(productState))

      service.currentState shouldBe productState

      service.get(api.GetProduct("P123")).reply shouldBe product
    }

    "handle ChangeName command" in {
      val service = ProductEntityTestKit(new ProductEntity(_))

      val product = api.Product(
        productId = "P123",
        productName = "Super Duper Thingamajig",
        price = Some(api.Money("USD", 123, 45)))

      val createResult = service.create(product)
      createResult.reply shouldBe Empty.defaultInstance

      val productState =
        ProductState(productId = "P123", productName = "Super Duper Thingamajig", price = Some(Money("USD", 123, 45)))

      service.currentState shouldBe productState

      service.get(api.GetProduct("P123")).reply shouldBe product

      val changeNameResult = service.changeName(api.ChangeProductName(productId = "P123", newName = "Thing Supreme"))
      changeNameResult.reply shouldBe Empty.defaultInstance

      changeNameResult.events.size shouldBe 1
      changeNameResult.nextEvent[ProductNameChanged] shouldBe ProductNameChanged(newName = "Thing Supreme")

      val productStateWithNewName = productState.withProductName("Thing Supreme")
      service.currentState shouldBe productStateWithNewName

      val productWithNewName = product.withProductName("Thing Supreme")
      service.get(api.GetProduct("P123")).reply shouldBe productWithNewName
    }

    "handle ChangePrice command" in {
      val service = ProductEntityTestKit(new ProductEntity(_))

      val product = api.Product(
        productId = "P123",
        productName = "Super Duper Thingamajig",
        price = Some(api.Money("USD", 123, 45)))

      val createResult = service.create(product)
      createResult.reply shouldBe Empty.defaultInstance

      val productState =
        ProductState(productId = "P123", productName = "Super Duper Thingamajig", price = Some(Money("USD", 123, 45)))

      service.currentState shouldBe productState

      service.get(api.GetProduct("P123")).reply shouldBe product

      val changePriceResult =
        service.changePrice(api.ChangeProductPrice(productId = "P123", newPrice = Some(api.Money("USD", 56, 78))))
      changePriceResult.reply shouldBe Empty.defaultInstance

      changePriceResult.events.size shouldBe 1
      changePriceResult.nextEvent[ProductPriceChanged] should be(
        ProductPriceChanged(newPrice = Some(Money("USD", 56, 78))))

      val productStateWithNewPrice = productState.withPrice(Money("USD", 56, 78))
      service.currentState shouldBe productStateWithNewPrice

      val productWithNewPrice = product.withPrice(api.Money("USD", 56, 78))
      service.get(api.GetProduct("P123")).reply shouldBe productWithNewPrice
    }

  }
}
