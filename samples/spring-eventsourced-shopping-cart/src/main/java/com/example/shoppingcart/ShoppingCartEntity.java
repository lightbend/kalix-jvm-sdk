package com.example.shoppingcart;

import com.example.shoppingcart.domain.ShoppingCart;
import com.example.shoppingcart.domain.ShoppingCart.LineItem;
import com.example.shoppingcart.domain.ShoppingCartEvent;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;
import kalix.springsdk.annotations.GenerateEntityKey;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

// tag::class[]
@EntityKey("cartId") // <2>
@EntityType("shopping-cart") // <3>
@RequestMapping("/cart") // <4>
public class ShoppingCartEntity extends EventSourcedEntity<ShoppingCart> { // <1>
  // end::class[]

// tag::getCart[]
  private final String entityId;

  public ShoppingCartEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId(); // <1>
  }

  @Override
  public ShoppingCart emptyState() { // <2>
    return new ShoppingCart(entityId, Collections.emptyList());
  }

  // end::getCart[]

  // tag::generateId[]
  @GenerateEntityKey // <1>
  @PostMapping("/create")
  public Effect<String> create() {
    return effects()
        .reply(commandContext().entityId()); // <2>
  }
  // end::generateId[]

  // tag::addItem[]
  @PostMapping("/{cartId}/add")
  public Effect<String> addItem(@RequestBody LineItem item) {
    if (item.quantity() <= 0) { // <1>
      return effects().error("Quantity for item " + item.productId() + " must be greater than zero.");
    }

    var event = new ShoppingCartEvent.ItemAdded(item); // <2>

    return effects()
        .emitEvent(event) // <3>
        .thenReply(newState -> "OK"); // <4>
  }

  // end::addItem[]

  @PostMapping("/{cartId}/items/{productId}/remove")
  public Effect<String> removeItem(@PathVariable String productId) {
    if (currentState().findItemByProductId(productId).isEmpty()) {
      return effects().error("Cannot remove item " + productId + " because it is not in the cart.");
    }

    var event = new ShoppingCartEvent.ItemRemoved(productId);

    return effects()
        .emitEvent(event)
        .thenReply(newState -> "OK");
  }

  // tag::getCart[]
  @GetMapping("/{cartId}") // <3>
  public Effect<ShoppingCart> getCart() {
    return effects().reply(currentState()); // <4>
  }
  // end::getCart[]

  // tag::addItem[]
  @EventHandler // <5>
  public ShoppingCart itemAdded(ShoppingCartEvent.ItemAdded itemAdded) {
    return currentState().onItemAdded(itemAdded); // <6>
  }

  // end::addItem[]

  @EventHandler
  public ShoppingCart itemRemoved(ShoppingCartEvent.ItemRemoved itemRemoved) {
    return currentState().onItemRemoved(itemRemoved);
  }
// tag::class[]

}
// end::class[]
