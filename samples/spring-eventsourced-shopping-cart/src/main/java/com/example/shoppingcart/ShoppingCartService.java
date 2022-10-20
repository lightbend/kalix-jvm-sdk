package com.example.shoppingcart;

import com.example.shoppingcart.domain.LineItem;
import com.example.shoppingcart.domain.ShoppingCart;
import com.example.shoppingcart.domain.ShoppingCartEvent;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.Entity;
import kalix.springsdk.annotations.EventHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// tag::class[]
@Entity(entityKey = "cartId", entityType = "shopping-cart")
@RequestMapping("/cart/{cartId}")
public class ShoppingCartService extends EventSourcedEntity<ShoppingCart> {

  private final String entityId;

  public ShoppingCartService(EventSourcedEntityContext context) { this.entityId = context.entityId(); }

  @Override
  public ShoppingCart emptyState() { // <2>
    return new ShoppingCart(entityId, Collections.emptyList());
  }
  // end::class[]

  // tag::addItem[]
  @PostMapping("/add")
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

  @PostMapping("/items/{productId}/remove")
  public Effect<String> removeItem(@PathVariable String productId) {
    if (findItemByProductId(currentState(), productId).isEmpty()) {
      return effects()
          .error(
              "Cannot remove item " + productId + " because it is not in the cart.");
    }

    var event = new ShoppingCartEvent.ItemRemoved(productId);

    return effects()
        .emitEvent(event)
        .thenReply(newState -> "OK");
  }

  // tag::getCart[]
  @GetMapping
  public Effect<ShoppingCart> getCart() {
    return effects().reply(currentState());
  }
  // end::getCart[]

  // tag::itemAdded[]
  @EventHandler
  public ShoppingCart itemAdded(ShoppingCartEvent.ItemAdded itemAdded) {
    var item = itemAdded.item();
    var lineItem = updateItem(item, currentState());
    List<LineItem> lineItems =
        removeItemByProductId(currentState(), item.productId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(LineItem::productId));
    return currentState().withItems(lineItems);
  }

  // end::itemAdded[]

  @EventHandler
  public ShoppingCart itemRemoved(ShoppingCartEvent.ItemRemoved itemRemoved) {
    List<LineItem> items =
        removeItemByProductId(currentState(), itemRemoved.productId());
    items.sort(Comparator.comparing(LineItem::productId));
    return currentState().withItems(items);
  }

  // tag::itemAdded[]
  private LineItem updateItem(
      LineItem item, ShoppingCart cart) {
    return findItemByProductId(cart, item.productId())
        .map(li -> li.withQuantity(li.quantity() + item.quantity()))
        .orElse(item);
  }

  private Optional<LineItem> findItemByProductId(
      ShoppingCart cart, String productId) {
    Predicate<LineItem> lineItemExists =
        lineItem -> lineItem.productId().equals(productId);
    return cart.items().stream().filter(lineItemExists).findFirst();
  }

  private List<LineItem> removeItemByProductId(
      ShoppingCart cart, String productId) {
    return cart.items().stream()
        .filter(lineItem -> !lineItem.productId().equals(productId))
        .collect(Collectors.toList());
  }
  // end::itemAdded[]

}
