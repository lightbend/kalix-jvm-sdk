package shoppingcart.api;


// tag::class[]

import kalix.javasdk.EntityContext;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.springframework.web.bind.annotation.*;
import shoppingcart.domain.ShoppingCart;
import shoppingcart.domain.ShoppingCart.Event.CheckedOut;
import shoppingcart.domain.ShoppingCart.Event.ItemAdded;
import shoppingcart.domain.ShoppingCart.Event.ItemRemoved;

import java.util.ArrayList;

@TypeId("shopping-cart") // <1>
@Id("cartId") // <2>
@RequestMapping("/cart/{cartId}") // <3>
public class ShoppingCartEntity
  extends EventSourcedEntity<ShoppingCart, ShoppingCart.Event> { // <4>

  final private String cartId;

  public ShoppingCartEntity(EventSourcedEntityContext entityContext) {
    this.cartId = entityContext.entityId();
  }

  @Override
  public ShoppingCart emptyState() { // <5>
    return new ShoppingCart(cartId, new ArrayList<>(), false);
  }

  @PostMapping("/add") // <6>
  public Effect<String> addItem(@RequestBody ShoppingCart.LineItem item) {
    if (currentState().checkedOut())
      return effects().error("Cart is already checked out.");

    if (item.quantity() <= 0) {
      return effects().error("Quantity for item " + item.productId() + " must be greater than zero.");
    }

    var event = new ItemAdded(item);

    return effects()
      .emitEvent(event) // <7>
      .thenReply(newState -> "OK");
  }


  @PostMapping("/items/{productId}/remove") // <6>
  public Effect<String> removeItem(@PathVariable String productId) {
    if (currentState().checkedOut())
      return effects().error("Cart is already checked out.");

    return effects()
      .emitEvent(new ItemRemoved(productId)) // <7>
      .thenReply(newState -> "OK");
  }

  @PostMapping("/checkout") // <6>
  public Effect<String> checkout() {
    if (currentState().checkedOut())
      return effects().error("Cart is already checked out.");

    return effects()
      .emitEvent(new CheckedOut()) // <7>
      .thenReply(newState -> "OK");
  }

  @GetMapping() // <6>
  public Effect<ShoppingCart> getCart() {
    return effects().reply(currentState());
  }

  @EventHandler // <8>
  public ShoppingCart itemAdded(ItemAdded itemAdded) {
    return currentState().addItem(itemAdded.item());
  }

  @EventHandler // <8>
  public ShoppingCart itemRemoved(ItemRemoved itemRemoved) {
    return currentState().removeItem(itemRemoved.productId());
  }

  @EventHandler // <8>
  public ShoppingCart checkedOut(CheckedOut checkedOut) {
    return currentState().checkOut();
  }
}
// end::class[]
