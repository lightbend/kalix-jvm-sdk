package com.example.shoppingcart;

import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.google.protobuf.Empty;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** An action. */
public class ShoppingCartActionImpl extends AbstractShoppingCartAction {

  public ShoppingCartActionImpl(ActionCreationContext creationContext) {}

  // tag::initialize[]
  @Override
  public Effect<ShoppingCartController.NewCartCreated> initializeCart(ShoppingCartController.NewCart newCart) {
    final String id = UUID.randomUUID().toString(); // <1>
    CompletionStage<Empty> shoppingCartCreated =
        components().shoppingCart().create(ShoppingCartApi.CreateCart.newBuilder().setCartId(id).build()) // <2>
        .execute(); // <3>

    // transform response
    CompletionStage<Effect<ShoppingCartController.NewCartCreated>> effect =
        shoppingCartCreated.handle((empty, error) -> { // <4>
          if (error == null) {
            return effects().reply(ShoppingCartController.NewCartCreated.newBuilder().setCartId(id).build()); // <5>
          } else {
            return effects().error("Failed to create cart, please retry"); // <6>
          }
        });

    return effects().asyncEffect(effect); // <7>
  }
  // end::initialize[]
}
