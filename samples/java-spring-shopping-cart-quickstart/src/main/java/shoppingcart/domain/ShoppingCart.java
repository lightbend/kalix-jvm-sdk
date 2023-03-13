package shoppingcart.domain;

import kalix.javasdk.annotations.TypeName;

import java.util.List;
import java.util.stream.Collectors;

public record ShoppingCart(String cartId, List<LineItem> items, boolean checkedOut) {


  public record LineItem(String productId, String name, int quantity) {
    public LineItem increaseQuantity(int qty) {
      return new LineItem(productId, name, quantity + qty);
    }
  }

  public ShoppingCart addItem(LineItem item) {
    var itemToAdd =
      items.stream()
        .filter(it -> it.productId.equals(item.productId))
        .findFirst()
        .map(it -> it.increaseQuantity(item.quantity))
        .orElse(item);

    return removeItem(item.productId).addAsNew(itemToAdd);
  }

  public ShoppingCart removeItem(String productId) {
    if (hasItem(productId)) {
      var updatedItems =
        items.stream()
          .filter(it -> !it.productId.equals(productId))
          .collect(Collectors.toList());

      return new ShoppingCart(cartId, updatedItems, checkedOut);
    } else {
      return this;
    }
  }


  private ShoppingCart addAsNew(LineItem item) {
    items.add(item);
    return this;
  }

  private boolean hasItem(String productId) {
    return items().stream().anyMatch(it -> it.productId.equals(productId));
  }

  public ShoppingCart checkOut() {
    return new ShoppingCart(cartId, items, true);
  }

  public sealed interface Event {

    @TypeName("item-added")
    record ItemAdded(ShoppingCart.LineItem item) implements Event {
    }

    @TypeName("item-removed")
    record ItemRemoved(String productId) implements Event {
    }

    @TypeName("checked-out")
    record CheckedOut() implements Event {
    }
  }
}
