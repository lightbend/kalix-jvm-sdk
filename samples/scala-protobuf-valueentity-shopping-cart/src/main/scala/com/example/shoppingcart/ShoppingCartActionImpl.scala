package com.example.shoppingcart

import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import com.google.protobuf.empty.Empty
import java.util.UUID

import scala.concurrent.Future

import kalix.scalasdk.Metadata
import kalix.scalasdk.action.Action.Effect

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ShoppingCartActionImpl(creationContext: ActionCreationContext) extends AbstractShoppingCartAction {

  // tag::initialize[]
  override def initializeCart(newCart: NewCart): Effect[NewCartCreated] = {
    val cartId = UUID.randomUUID().toString // <1>

    val created: Future[Empty] =
      components.shoppingCart.create(CreateCart(cartId)).execute() // <2>

    val effect: Future[Effect[NewCartCreated]] = // <3>
      created
        .map(_ => effects.reply(NewCartCreated(cartId))) // <4>
        .recover(_ => effects.error("Failed to create cart, please retry")) // <5>

    effects.asyncEffect(effect) // <6>
  }
  // end::initialize[]

  // tag::forward[]
  override def verifiedAddItem(addLineItem: AddLineItem): Effect[Empty] =
    if (addLineItem.name.equalsIgnoreCase("carrot")) // <1>
      effects.error("Carrots no longer for sale") // <2>
    else {
      val call = components.shoppingCart.addItem(addLineItem) // <3>
      effects.forward(call) // <4>
    }
  // end::forward[]

  // tag::createPrePopulated[]
  def createPrePopulated(newCart: NewCart): Effect[NewCartCreated] = {
    val cartId = UUID.randomUUID().toString

    val reply: Future[NewCartCreated] =
      for { // <1>
        created <- components.shoppingCart.create(CreateCart(cartId)).execute()
        populated <- components.shoppingCart.addItem(AddLineItem(cartId, "e", "eggplant", 1)).execute()
      } yield NewCartCreated(cartId) // <2>

    effects.asyncReply(reply) // <3>
  }
  // end::createPrePopulated[]

  // tag::unsafeValidation[]
  override def unsafeValidation(addLineItem: AddLineItem): Effect[Empty] = {
    // NOTE: This is an example of an anti-pattern, do not copy this
    val cartReply = components.shoppingCart
      .getCart(GetShoppingCart(addLineItem.cartId))
      .execute() // <1>

    val effect =
      cartReply.map { cart =>
        val totalCount = cart.items.map(_.quantity).sum

        if (totalCount < 10)
          effects.error("Max 10 items in a cart")
        else
          effects.forward(components.shoppingCart.addItem(addLineItem)) // <2>
      }

    effects.asyncEffect(effect)
  }
  // end::unsafeValidation[]

  // tag::forward-headers[]
  override def removeCart(removeShoppingCart: RemoveShoppingCart): Effect[Empty] = {
    val userRole = actionContext.metadata.get("UserRole").get // <1>
    val metadata = Metadata.empty.add("Role", userRole)
    effects.forward(
      components.shoppingCart
        .removeCart(removeShoppingCart)
        .withMetadata(metadata)) // <2>
  }
  // end::forward-headers[]
}
