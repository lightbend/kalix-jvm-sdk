package com.example.shoppingcart

// tag::sample-it-test[]
import kalix.scalasdk.testkit.KalixTestKit
// ...

// end::sample-it-test[]
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::sample-it-test[]
class ShoppingCartServiceIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start() // <1>
  import testKit.executionContext

  "ShoppingCartService" must {
    val client = testKit.getGrpcClient(classOf[ShoppingCartService]) // <2>

    "add items to shopping cart" in {
      val cartId = "cart1"

      val updatedCart = for {
        _ <- client.addItem(AddLineItem(cartId, "shirt", "Shirt", 1))
        done <- client.addItem(AddLineItem(cartId, "sweat", "Sweat Shirt", 2))
      } yield done

      updatedCart.futureValue

      client.getCart(GetShoppingCart(cartId)).futureValue.items shouldBe // <3>
        Seq(LineItem("shirt", "Shirt", 1), LineItem("sweat", "Sweat Shirt", 2))
    }

  }

  override def afterAll() = { // <4>
    testKit.stop()
    super.afterAll()
  }
}
// end::sample-it-test[]
