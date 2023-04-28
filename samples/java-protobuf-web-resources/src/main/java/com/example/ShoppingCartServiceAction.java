package com.example;

import com.google.protobuf.Empty;
import kalix.javasdk.action.ActionCreationContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/web_resources.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class ShoppingCartServiceAction extends AbstractShoppingCartServiceAction {

  public ShoppingCartServiceAction(ActionCreationContext creationContext) {}

  @Override
  public Effect<WebResources.ShoppingCart> getCart(Empty empty) {
    return effects().reply(WebResources.ShoppingCart.newBuilder()
            .setCartId("my-card")
                    .addItems("Banana")
                    .addItems("Apple")
            .build());

  }
}
