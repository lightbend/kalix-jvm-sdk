/*
 * Copyright 2021 Lightbend Inc.
 */

package valueentity.shoppingcart;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.example.valueentity.shoppingcart.ShoppingCartApi;
import com.example.valueentity.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** A value based entity. */
@ValueEntity(entityType = "shopping-cart")
public class ShoppingCartEntity {

  private final String entityId;

  public ShoppingCartEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }

  // tag::get-cart[]
  @CommandHandler
  public ShoppingCartApi.Cart getCart(CommandContext<ShoppingCartDomain.Cart> ctx) {
    ShoppingCartDomain.Cart cart =
        ctx.getState().orElse(ShoppingCartDomain.Cart.newBuilder().build());
    List<ShoppingCartApi.LineItem> allItems =
        cart.getItemsList().stream()
            .map(this::convert)
            .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
            .collect(Collectors.toList());
    return ShoppingCartApi.Cart.newBuilder().addAllItems(allItems).build();
  }
  // end::get-cart[]

  // tag::add-item[]
  @CommandHandler
  public Empty addItem(
      ShoppingCartApi.AddLineItem item, CommandContext<ShoppingCartDomain.Cart> ctx) {
    if (item.getQuantity() <= 0) {
      throw ctx.fail("Cannot add negative quantity of to item " + item.getProductId());
    }

    ShoppingCartDomain.Cart cart =
        ctx.getState().orElse(ShoppingCartDomain.Cart.newBuilder().build());
    ShoppingCartDomain.LineItem lineItem = updateItem(item, cart);
    List<ShoppingCartDomain.LineItem> lineItems = removeItemByProductId(cart, item.getProductId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    ctx.updateState(ShoppingCartDomain.Cart.newBuilder().addAllItems(lineItems).build());
    return Empty.getDefaultInstance();
  }
  // end::add-item[]

  @CommandHandler
  public Empty removeItem(
      ShoppingCartApi.RemoveLineItem item, CommandContext<ShoppingCartDomain.Cart> ctx) {
    ShoppingCartDomain.Cart cart =
        ctx.getState().orElse(ShoppingCartDomain.Cart.newBuilder().build());
    Optional<ShoppingCartDomain.LineItem> lineItem = findItemByProductId(cart, item.getProductId());

    if (!lineItem.isPresent()) {
      throw ctx.fail(
          "Cannot remove item " + item.getProductId() + " because it is not in the cart.");
    }

    List<ShoppingCartDomain.LineItem> items = removeItemByProductId(cart, item.getProductId());
    items.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    ctx.updateState(ShoppingCartDomain.Cart.newBuilder().addAllItems(items).build());
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty removeCart(
      ShoppingCartApi.RemoveShoppingCart cart, CommandContext<ShoppingCartDomain.Cart> ctx) {
    ctx.deleteState();
    return Empty.getDefaultInstance();
  }

  private ShoppingCartDomain.LineItem updateItem(
      ShoppingCartApi.AddLineItem item, ShoppingCartDomain.Cart cart) {
    return findItemByProductId(cart, item.getProductId())
        .map(li -> li.toBuilder().setQuantity(li.getQuantity() + item.getQuantity()).build())
        .orElse(newItem(item));
  }

  private ShoppingCartDomain.LineItem newItem(ShoppingCartApi.AddLineItem item) {
    return ShoppingCartDomain.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }

  private Optional<ShoppingCartDomain.LineItem> findItemByProductId(
      ShoppingCartDomain.Cart cart, String productId) {
    Predicate<ShoppingCartDomain.LineItem> lineItemExists =
        lineItem -> lineItem.getProductId().equals(productId);
    return cart.getItemsList().stream().filter(lineItemExists).findFirst();
  }

  private List<ShoppingCartDomain.LineItem> removeItemByProductId(
      ShoppingCartDomain.Cart cart, String productId) {
    return cart.getItemsList().stream()
        .filter(lineItem -> !lineItem.getProductId().equals(productId))
        .collect(Collectors.toList());
  }

  private ShoppingCartApi.LineItem convert(ShoppingCartDomain.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
}
