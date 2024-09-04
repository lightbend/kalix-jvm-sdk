package com.example.shoppingcart

import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ShoppingCartServiceIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()

  private val client = testKit.getGrpcClient(classOf[ShoppingCartService])
  private val actionClient = testKit.getGrpcClient(classOf[ShoppingCartAction])

  "ShoppingCartService" must {

    "be empty initially" in {
      client.getCart(GetShoppingCart("cart1")).futureValue.items.size shouldBe 0
    }

    "add items to cart" in {
      client.addItem(AddLineItem("cart2", "a", "Apple", 1)).futureValue
      client.addItem(AddLineItem("cart2", "b", "Banana", 2)).futureValue
      client.addItem(AddLineItem("cart2", "c", "Cantaloupe", 3)).futureValue
      val cart = client.getCart(GetShoppingCart("cart2")).futureValue
      cart.items should have size 3
      cart.items shouldBe Seq(LineItem("a", "Apple", 1), LineItem("b", "Banana", 2), LineItem("c", "Cantaloupe", 3))
    }

    "remove items from cart" in {
      client.addItem(AddLineItem("cart3", "a", "Apple", 1)).futureValue
      client.addItem(AddLineItem("cart3", "b", "Banana", 2)).futureValue
      val cart1 = client.getCart(GetShoppingCart("cart3")).futureValue
      cart1.items should have size 2
      client.removeItem(RemoveLineItem("cart3", "a")).futureValue
      val cart2 = client.getCart(GetShoppingCart("cart3")).futureValue
      cart2.items should have size 1
      cart2.items.head shouldBe LineItem("b", "Banana", 2)
    }

    "only create new cart once" in {
      val created = actionClient.initializeCart(NewCart.defaultInstance).futureValue
      val cartId = created.cartId

      val cart = client.getCart(GetShoppingCart(cartId)).futureValue
      cart.creationTimestamp shouldBe >(0L)
    }

    "create new populated cart" in {
      val created = actionClient.createPrePopulated(NewCart.defaultInstance).futureValue
      val cart = client.getCart(GetShoppingCart(created.cartId)).futureValue
      cart.creationTimestamp shouldBe >(0L)
      cart.items should have size 1
    }

    "not allow adding carrots" in {
      val cartId = "carrot-cart"
      actionClient.verifiedAddItem(AddLineItem(cartId, "c", "Carrot", 4)).failed.futureValue
      actionClient.verifiedAddItem(AddLineItem(cartId, "b", "Banana", 1)).futureValue
      val cart = client.getCart(GetShoppingCart(cartId)).futureValue
      cart.items should have size 1
    }

    "remove cart with proper user role" in {
      val cartId = "cart-5"
      actionClient.verifiedAddItem(AddLineItem(cartId, "b", "Banana", 1)).futureValue
      val cart = client.getCart(GetShoppingCart(cartId)).futureValue
      cart.items should have size 1
      actionClient
        .asInstanceOf[ShoppingCartActionClient]
        .removeCart()
        .addHeader("UserRole", "Admin")
        .invoke(RemoveShoppingCart(cartId))
        .futureValue

      val cart2 = client.getCart(GetShoppingCart(cartId)).futureValue
      cart2.items should have size 0
    }

    "not delete cart when user role is missing" in {
      val cartId = "cart-6"
      actionClient.verifiedAddItem(AddLineItem(cartId, "b", "Banana", 1)).futureValue
      val cart = client.getCart(GetShoppingCart(cartId)).futureValue
      cart.items should have size 1
      actionClient.removeCart(RemoveShoppingCart(cartId)).failed.futureValue
      val cart2 = client.getCart(GetShoppingCart(cartId)).futureValue
      cart2.items should have size 1
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
