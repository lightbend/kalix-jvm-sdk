package com.example.shoppingcart

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.google.protobuf.empty.Empty

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
class ShoppingCartActionImpl(creationContext: ActionCreationContext) extends AbstractShoppingCartAction {

  // tag::initialize[]
  override def initializeCart(newCart: NewCart): Action.Effect[NewCartCreated] = {
    val cartId = UUID.randomUUID().toString // <1>

    val created: Future[Empty] =
      components.shoppingCart.create(CreateCart(cartId)).execute() // <2>

    val effect: Future[Action.Effect[NewCartCreated]] = // <3>
      created.map(_ => effects.reply(NewCartCreated(cartId))) // <4>
        .recover(_ => effects.error("Failed to create cart, please retry")) // <5>

    effects.asyncEffect(effect) // <6>
  }
  // end::initialize[]
}

