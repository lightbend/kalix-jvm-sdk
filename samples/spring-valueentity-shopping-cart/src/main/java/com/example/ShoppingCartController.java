package com.example;

import com.example.api.ShoppingCartDTO;
import com.example.domain.ShoppingCart;
import com.google.protobuf.any.Any;
import com.google.protobuf.Empty;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.springsdk.KalixClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@RequestMapping("/carts")
public class ShoppingCartController extends Action {

  private KalixClient kalixClient;

  public ShoppingCartController(ActionCreationContext creationContext, KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  // tag::initialize[]
  @PostMapping("/create")
  public Action.Effect<String> initializeCart() {
    final String cartId = UUID.randomUUID().toString(); // <1>
    CompletionStage<ShoppingCartDTO> shoppingCartCreated =
        kalixClient.post("/cart/" + cartId + "/create", "", ShoppingCartDTO.class).execute(); // <3>

    // transform response
    CompletionStage<Action.Effect<String>> effect =
        shoppingCartCreated.handle((empty, error) -> { // <4>
          if (error == null) {
            return effects().reply(cartId); // <5>
          } else {
            return effects().error("Failed to create cart, please retry"); // <6>
          }
        });

    return effects().asyncEffect(effect); // <7>
  }
  // end::initialize[]

  // tag::forward[]
  @PostMapping("/{cartId}/items/add")
  public Action.Effect<ShoppingCartDTO> verifiedAddItem(@PathVariable String cartId, @RequestBody ShoppingCart.LineItem addLineItem) {
    if (addLineItem.name().equalsIgnoreCase("carrot")) { // <1>
      return effects().error("Carrots no longer for sale"); // <2>
    } else {
      DeferredCall<Any, ShoppingCartDTO> call =
          kalixClient.post("/cart/" + cartId + "/items/add", addLineItem, ShoppingCartDTO.class); // <3>
      return effects().forward(call); // <4>
    }
  }
  // end::forward[]


  // tag::createPrePopulated[]
  @PostMapping("/prepopulated")
  public Action.Effect<ShoppingCartDTO> createPrePopulated() {
    final String cartId = UUID.randomUUID().toString();
    CompletionStage<ShoppingCart> shoppingCartCreated =
        kalixClient.post("/cart/" + cartId + "/create", "", ShoppingCart.class).execute();

    CompletionStage<ShoppingCartDTO> cartPopulated = shoppingCartCreated.thenCompose(empty -> { // <1>
      ShoppingCart.LineItem initialItem = new ShoppingCart.LineItem("e", "eggplant", 1);

      return kalixClient.post("/cart/" + cartId + "/items/add", initialItem, ShoppingCartDTO.class).execute(); // <3>
    });

    return effects()
        .asyncReply(cartPopulated.thenApply(reply -> reply)); // <5>
  }
  // end::createPrePopulated[]

  // tag::unsafeValidation[]
  @PostMapping("/{cartId}/unsafeAddItem")
  public Action.Effect<String> unsafeValidation(@PathVariable String cartId, @RequestBody ShoppingCart.LineItem addLineItem) {
    // NOTE: This is an example of an anti-pattern, do not copy this
    CompletionStage<ShoppingCart> cartReply =
        kalixClient.get("/cart/" + cartId, ShoppingCart.class).execute(); // <1>

    CompletionStage<Action.Effect<String>> effect = cartReply.thenApply(cart -> {
      int totalCount = cart.items().stream()
          .mapToInt(ShoppingCart.LineItem::quantity)
          .sum();

      if (totalCount < 10) {
        return effects().error("Max 10 items in a cart");
      } else {
        var addCall = kalixClient.post("/cart/" + cartId + "/items/add", addLineItem, String.class);
        return effects()
            .forward(addCall); // <2>
      }
    });

    return effects().asyncEffect(effect);
  }
  // end::unsafeValidation[]
}
