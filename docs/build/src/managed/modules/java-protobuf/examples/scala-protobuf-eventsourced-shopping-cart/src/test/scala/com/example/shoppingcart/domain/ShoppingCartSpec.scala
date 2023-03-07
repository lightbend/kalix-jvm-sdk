package com.example.shoppingcart.domain

import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.testkit.EventSourcedResult
import com.example.shoppingcart
import com.example.shoppingcart.AddLineItem
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ShoppingCartSpec extends AnyWordSpec with Matchers {
  "The ShoppingCart" should {

    "correctly process commands of type AddItem" in {
      val testKit = ShoppingCartTestKit(new ShoppingCart(_)) // <1>
      val apples = AddLineItem(productId = "idA", name = "apples", quantity = 1)
      val addingApplesResult = testKit.addItem(apples) // <2>

      val bananas = AddLineItem(productId = "idB", name = "bananas", quantity = 2)
      testKit.addItem(bananas) // <3>

      addingApplesResult.events should have size 1 // <4>
      testKit.allEvents should have size 2 // <5>

      val addedApples = addingApplesResult.nextEvent[ItemAdded] // <6>
      addedApples.getItem.name shouldBe "apples"
      intercept[NoSuchElementException] { // <7>
        addingApplesResult.nextEvent[ItemAdded]
      }
      addingApplesResult.reply shouldBe Empty.defaultInstance // <8>

      val expectedState = Cart(Seq(
        LineItem(productId = "idA", name = "apples", quantity = 1),
        LineItem(productId = "idB", name = "bananas", quantity = 2)
      ))
      testKit.currentState shouldBe expectedState // <9>
    }
  }
}
