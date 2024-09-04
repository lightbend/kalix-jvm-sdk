package com.example.shoppingcart.domain

import com.example.shoppingcart
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ShoppingCartSpec
    extends AnyWordSpec
    with Matchers {

  "ShoppingCart" must {

    "handle command Create" in {
      val testKit = ShoppingCartTestKit(new ShoppingCart(_))
      val creation1Result = testKit.create(shoppingcart.CreateCart())
      creation1Result.reply shouldBe Empty.defaultInstance
      testKit.currentState().creationTimestamp shouldBe >(0L)

      // creating an already created cart is not allowed
      val creation2Result = testKit.create(shoppingcart.CreateCart())
      creation2Result.isError shouldBe true
    }

    "handle command AddItem" in {
      val testKit = ShoppingCartTestKit(new ShoppingCart(_))

      val creation1Result = testKit.create(shoppingcart.CreateCart())
      creation1Result.reply shouldBe Empty.defaultInstance
      val creationTimestamp = testKit.currentState().creationTimestamp

       val resultA = testKit.addItem(shoppingcart.AddLineItem(productId = "idA", name = "nameA", quantity = 1))
        resultA.reply shouldBe Empty.defaultInstance
      resultA.stateWasUpdated shouldBe true

      val resultB = testKit.addItem(shoppingcart.AddLineItem(productId = "idB", name = "nameB", quantity = 2))
      resultB.reply shouldBe Empty.defaultInstance
      resultB.stateWasUpdated shouldBe true

      val expectedState = Cart(
        creationTimestamp = creationTimestamp,
        items = Seq(
          LineItem(productId = "idA", name = "nameA", quantity = 1),
          LineItem(productId = "idB", name = "nameB", quantity = 2)
        ))

      testKit.currentState() shouldBe expectedState
    }
  }
}
