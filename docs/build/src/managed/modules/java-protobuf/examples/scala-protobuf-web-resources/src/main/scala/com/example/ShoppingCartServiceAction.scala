package com.example

import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ShoppingCartServiceAction(creationContext: ActionCreationContext) extends AbstractShoppingCartServiceAction {

  override def getCart(empty: Empty): Action.Effect[ShoppingCart] = {
    effects.reply(ShoppingCart.of("my-card", Seq("Banana", "Apple")))
  }
}

