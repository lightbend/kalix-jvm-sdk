package com.example.shoppingcart;

import com.example.shoppingcart.domain.ShoppingCart;
import com.example.shoppingcart.domain.ShoppingCart.LineItem;
import com.example.shoppingcart.domain.ShoppingCartEvent;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.EventHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

// tag::class[]
@Id("cartId") // <2>
@TypeId("shopping-cart") // <3>
@RequestMapping("/cart/{cartId}") // <4>
public class ShoppingCartEntity extends EventSourcedEntity<ShoppingCart, ShoppingCartEvent> { // <1>
  // end::class[]

// tag::getCart[]
  private final String entityId;

  public ShoppingCartEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId(); // <1>
  }

  @Override
  public ShoppingCart emptyState() { // <2>
    return new ShoppingCart(entityId, Collections.emptyList(), false);
  }

  // end::getCart[]

  @PostMapping("/create")
  public Effect<String> create() {
    return effects().reply("OK");
  }

  // tag::addItem[]
  @PostMapping("/add")
  public Effect<String> addItem(@RequestBody LineItem item) {
    if (currentState().checkedOut())
      return effects().error("Cart is already checked out.");
    if (item.quantity() <= 0) { // <1>
      return effects().error("Quantity for item " + item.productId() + " must be greater than zero.");
    }

    var event = new ShoppingCartEvent.ItemAdded(item); // <2>

    return effects()
        .emitEvent(event) // <3>
        .thenReply(newState -> "OK"); // <4>
  }

  // end::addItem[]

  @PostMapping("/items/{productId}/remove")
  public Effect<String> removeItem(@PathVariable String productId) {
    if (currentState().checkedOut())
      return effects().error("Cart is already checked out.");
    if (currentState().findItemByProductId(productId).isEmpty()) {
      return effects().error("Cannot remove item " + productId + " because it is not in the cart.");
    }

    var event = new ShoppingCartEvent.ItemRemoved(productId);

    return effects()
        .emitEvent(event)
        .thenReply(newState -> "OK");
  }

  // tag::getCart[]
  @GetMapping() // <3>
  public Effect<ShoppingCart> getCart() {
    return effects().reply(currentState()); // <4>
  }
  // end::getCart[]

  // tag::checkout[]
  @PostMapping("/checkout")
  public Effect<String> checkout() {
    if (currentState().checkedOut())
      return effects().error("Cart is already checked out.");

    return effects()
        .emitEvent(new ShoppingCartEvent.CheckedOut()) // <1>
        .deleteEntity() // <2>
        .thenReply(newState -> "OK"); // <4>
  }

  // end::checkout[]

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

  @EventHandler
  public ShoppingCart checkedOut(ShoppingCartEvent.CheckedOut checkedOut) {
    return currentState().onCheckedOut();
  }
// tag::class[]

}
// end::class[]
