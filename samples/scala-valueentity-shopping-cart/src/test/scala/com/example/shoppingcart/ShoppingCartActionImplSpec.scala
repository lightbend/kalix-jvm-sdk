package com.example.shoppingcart

import com.google.protobuf.empty.Empty
import kalix.scalasdk.testkit.MockRegistry
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ShoppingCartActionImplSpec
    extends AsyncWordSpec
    with Matchers
    with AsyncMockFactory {

  "ShoppingCartActionImpl" must {

    "initialize cart" in {
      val mockShoppingCart = mock[ShoppingCartService]
      (mockShoppingCart.create _)
        .expects(*)
        .returning(Future.successful(Empty.defaultInstance))
      val mockRegistry = MockRegistry.withMock(mockShoppingCart)

      val service = ShoppingCartActionImplTestKit(new ShoppingCartActionImpl(_), mockRegistry)
      val cartId = service.initializeCart(NewCart.defaultInstance).asyncResult

      cartId.map { newCart => assert(newCart.reply.cartId.nonEmpty) }
    }

    "create a prepopulated cart" in {
      val mockShoppingCart = stub[ShoppingCartService]
      (mockShoppingCart.create _)
        .when(*)
        .returns(Future.successful(Empty.defaultInstance))
      (mockShoppingCart.addItem _)
        .when(where { li: AddLineItem => li.name == "eggplant"})
        .returns(Future.successful(Empty.defaultInstance))
      val mockRegistry = MockRegistry.withMock(mockShoppingCart)

      val service = ShoppingCartActionImplTestKit(new ShoppingCartActionImpl(_), mockRegistry)
      val cartId = service.createPrePopulated(NewCart.defaultInstance).asyncResult

      cartId.map { newCart =>
        (mockShoppingCart.create _)
          .verify(where { c: CreateCart => c.cartId == newCart.reply.cartId })
          .once()
        (mockShoppingCart.addItem _)
          .verify(where { li: AddLineItem => li.cartId == newCart.reply.cartId })
          .once()
        succeed
      }
    }
  }
}
