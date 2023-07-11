package com.example.shoppingcart;

import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.ActionCreationContext;
import com.google.protobuf.Empty;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class ShoppingCartActionImpl extends AbstractShoppingCartAction {

  public ShoppingCartActionImpl(ActionCreationContext creationContext) {}

  // tag::initialize[]
  @Override
  public Effect<ShoppingCartController.NewCartCreated> initializeCart(ShoppingCartController.NewCart newCart) {
    final String cartId = UUID.randomUUID().toString(); // <1>
    CompletionStage<Empty> shoppingCartCreated =
        components().shoppingCart().create(ShoppingCartApi.CreateCart.newBuilder().setCartId(cartId).build()) // <2>
        .execute(); // <3>

    // transform response
    CompletionStage<Effect<ShoppingCartController.NewCartCreated>> effect =
        shoppingCartCreated.handle((empty, error) -> { // <4>
          if (error == null) {
            return effects().reply(ShoppingCartController.NewCartCreated.newBuilder().setCartId(cartId).build()); // <5>
          } else {
            return effects().error("Failed to create cart, please retry"); // <6>
          }
        });

    return effects().asyncEffect(effect); // <7>
  }
  // end::initialize[]

  // tag::forward[]
  @Override
  public Effect<Empty> verifiedAddItem(ShoppingCartApi.AddLineItem addLineItem) {
    if (addLineItem.getName().equalsIgnoreCase("carrot")) { // <1>
      return effects().error("Carrots no longer for sale"); // <2>
    } else {
      DeferredCall<ShoppingCartApi.AddLineItem, Empty> call =
          components().shoppingCart().addItem(addLineItem); // <3>
      return effects().forward(call); // <4>
    }
  }
  // end::forward[]


  // tag::createPrePopulated[]
  @Override
  public Effect<ShoppingCartController.NewCartCreated> createPrePopulated(ShoppingCartController.NewCart newCart) {
    final String cartId = UUID.randomUUID().toString();
    CompletionStage<Empty> shoppingCartCreated =
        components().shoppingCart().create(ShoppingCartApi.CreateCart.newBuilder().setCartId(cartId).build())
            .execute();

    CompletionStage<Empty> cartPopulated = shoppingCartCreated.thenCompose(empty -> { // <1>
      ShoppingCartApi.AddLineItem initialItem = // <2>
          ShoppingCartApi.AddLineItem.newBuilder()
              .setCartId(cartId)
              .setProductId("e")
              .setName("eggplant")
              .setQuantity(1)
              .build();

      return components().shoppingCart().addItem(initialItem).execute(); // <3>
    });

    CompletionStage<ShoppingCartController.NewCartCreated> reply =
        cartPopulated.thenApply(empty -> // <4>
          ShoppingCartController.NewCartCreated.newBuilder().setCartId(cartId).build()
        );

    return effects().asyncReply(reply); // <5>
  }
  // end::createPrePopulated[]

  // tag::unsafeValidation[]
  @Override
  public Effect<Empty> unsafeValidation(ShoppingCartApi.AddLineItem addLineItem) {
    // NOTE: This is an example of an anti-pattern, do not copy this
    CompletionStage<ShoppingCartApi.Cart> cartReply = components().shoppingCart().getCart(
            ShoppingCartApi.GetShoppingCart.newBuilder()
                .setCartId(addLineItem.getCartId())
                .build())
        .execute(); // <1>

    CompletionStage<Effect<Empty>> effect = cartReply.thenApply(cart -> {
      int totalCount = cart.getItemsList().stream()
          .mapToInt(ShoppingCartApi.LineItem::getQuantity)
          .sum();

      if (totalCount < 10) {
        return effects().error("Max 10 items in a cart");
      } else {
        return effects().forward(components().shoppingCart().addItem(addLineItem)); // <2>
      }
    });

    return effects().asyncEffect(effect);
  }
  // end::unsafeValidation[]

  // tag::forward-headers[]
  @Override
  public Effect<Empty> removeCart(ShoppingCartApi.RemoveShoppingCart removeShoppingCart) {
    var userRole = actionContext().metadata().get("UserRole").get(); // <1>
    Metadata metadata = Metadata.EMPTY.add("Role", userRole);
    return effects().forward(
        components().shoppingCart().removeCart(removeShoppingCart)
            .withMetadata(metadata) // <2>
    );
  }
  // end::forward-headers[]
}
