package com.example.api;

import com.example.api.ShoppingCartDTO.LineItemDTO;
// tag::forward[]
import kalix.javasdk.action.Action;
import kalix.spring.KalixClient;
// end::forward[]
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

// tag::forward[]

@RequestMapping("/carts")
public class ShoppingCartController extends Action {

  private final KalixClient kalixClient;

  public ShoppingCartController(KalixClient kalixClient) {
    this.kalixClient = kalixClient; // <1>
  }

  // end::forward[]

  // tag::initialize[]
  @PostMapping("/create")
  public Action.Effect<String> initializeCart() {
    final String cartId = UUID.randomUUID().toString(); // <1>
    CompletionStage<ShoppingCartDTO> shoppingCartCreated =
        kalixClient
            .post("/cart/" + cartId + "/create", "", ShoppingCartDTO.class) // <2>
            .execute(); // <3>

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
  @PostMapping("/{cartId}/items/add") // <2>
  public Action.Effect<ShoppingCartDTO> verifiedAddItem(@PathVariable String cartId,
                                                        @RequestBody LineItemDTO addLineItem) {
    if (addLineItem.name().equalsIgnoreCase("carrot")) { // <3>
      return effects().error("Carrots no longer for sale"); // <4>
    } else {
      var deferredCall =
          kalixClient.post("/cart/" + cartId + "/items/add", addLineItem, ShoppingCartDTO.class); // <5>
      return effects().forward(deferredCall); // <6>
    }
  }
  // end::forward[]


  // tag::createPrePopulated[]
  @PostMapping("/prepopulated")
  public Action.Effect<String> createPrePopulated() {
    final String cartId = UUID.randomUUID().toString();
    CompletionStage<ShoppingCartDTO> shoppingCartCreated =
        kalixClient.post("/cart/" + cartId + "/create", "", ShoppingCartDTO.class).execute();

    CompletionStage<ShoppingCartDTO> cartPopulated =
        shoppingCartCreated.thenCompose(empty -> { // <1>
          var initialItem = new LineItemDTO("e", "eggplant", 1);

          return kalixClient
              .post("/cart/" + cartId + "/items/add", initialItem, ShoppingCartDTO.class) // <2>
              .execute(); // <3>
        });

    CompletionStage<String> reply = cartPopulated.thenApply(ShoppingCartDTO::cartId); // <4>

    return effects()
        .asyncReply(reply); // <5>
  }
  // end::createPrePopulated[]

  // tag::unsafeValidation[]
  @PostMapping("/{cartId}/unsafeAddItem")
  public Action.Effect<String> unsafeValidation(@PathVariable String cartId,
                                                @RequestBody LineItemDTO addLineItem) {
    // NOTE: This is an example of an anti-pattern, do not copy this
    CompletionStage<ShoppingCartDTO> cartReply =
        kalixClient.get("/cart/" + cartId, ShoppingCartDTO.class).execute(); // <1>

    CompletionStage<Action.Effect<String>> effect = cartReply.thenApply(cart -> {
      int totalCount = cart.items().stream()
          .mapToInt(LineItemDTO::quantity)
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
