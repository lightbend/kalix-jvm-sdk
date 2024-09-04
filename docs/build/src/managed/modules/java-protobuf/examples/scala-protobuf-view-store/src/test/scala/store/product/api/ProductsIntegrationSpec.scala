package store.product.api

import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec
import store.Main

class ProductsIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix(), KalixTestKit.DefaultSettings.withAdvancedViews()).start()

  private val products = testKit.getGrpcClient(classOf[Products])

  "Products" must {

    "create and get product" in {
      val product =
        Product(productId = "P123", productName = "Super Duper Thingamajig", price = Some(Money("USD", 123, 45)))
      products.create(product).futureValue
      products.get(GetProduct("P123")).futureValue shouldBe product
    }

    "change product name" in {
      val product =
        Product(productId = "P234", productName = "Super Duper Thingamajig", price = Some(Money("USD", 123, 45)))
      products.create(product).futureValue
      products.get(GetProduct("P234")).futureValue shouldBe product

      val newName = "Thing Supreme"
      products.changeName(ChangeProductName(productId = "P234", newName = newName)).futureValue
      products.get(GetProduct("P234")).futureValue shouldBe product.withProductName(newName)
    }

    "change product price" in {
      val product =
        Product(productId = "P345", productName = "Super Duper Thingamajig", price = Some(Money("USD", 123, 45)))
      products.create(product).futureValue
      products.get(GetProduct("P345")).futureValue shouldBe product

      val newPrice = Money("USD", 56, 78)
      products
        .changePrice(ChangeProductPrice(productId = "P345", newPrice = Some(newPrice)))
        .futureValue
      products.get(GetProduct("P345")).futureValue shouldBe product.withPrice(newPrice)
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
