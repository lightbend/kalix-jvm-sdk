package com.example.shoppingcart.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// tag::domain[]
public record ShoppingCart(String cartId, List<LineItem> items) { // <1>

  public record LineItem(String productId, String name, int quantity) { // <2>
    // end::domain[]
    public LineItem withQuantity(int quantity) {
      return new LineItem(productId, name, quantity);
    }
    // tag::domain[]
  }
  // end::domain[]

  // tag::itemAdded[]
  public ShoppingCart onItemAdded(ShoppingCartEvent.ItemAdded itemAdded) {
    var item = itemAdded.item();
    var lineItem = updateItem(item, this);
    List<LineItem> lineItems =
        removeItemByProductId(this, item.productId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(LineItem::productId));
    return new ShoppingCart(cartId, lineItems);
  }
  // end::itemAdded[]

  public ShoppingCart onItemRemoved(ShoppingCartEvent.ItemRemoved itemRemoved) {
    List<LineItem> updatedItems =
        removeItemByProductId(this, itemRemoved.productId());
    updatedItems.sort(Comparator.comparing(LineItem::productId));
    return new ShoppingCart(cartId, updatedItems);
  }

  // tag::itemAdded[]

  private static LineItem updateItem(LineItem item, ShoppingCart cart) {
    return cart.findItemByProductId(item.productId())
        .map(li -> li.withQuantity(li.quantity() + item.quantity()))
        .orElse(item);
  }

  public Optional<LineItem> findItemByProductId(String productId) {
    Predicate<LineItem> lineItemExists =
        lineItem -> lineItem.productId().equals(productId);
    return items.stream().filter(lineItemExists).findFirst();
  }

  private static List<LineItem> removeItemByProductId(
      ShoppingCart cart, String productId) {
    return cart.items().stream()
        .filter(lineItem -> !lineItem.productId().equals(productId))
        .collect(Collectors.toList());
  }
  // end::itemAdded[]
  // tag::domain[]
}

// end::domain[]