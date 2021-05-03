/*
 * Copyright 2021 Lightbend Inc.
 */

package eventsourced.shoppingcart;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** An event sourced entity. */
@EventSourcedEntity(entityType = "eventsourced-shopping-cart")
public class ShoppingCartEntity {
  private final String entityId;
  private final Map<String, ShoppingCartApi.LineItem> cart = new LinkedHashMap<>();

  public ShoppingCartEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }

  @Snapshot
  public ShoppingCartDomain.Cart snapshot() {
    return ShoppingCartDomain.Cart.newBuilder()
        .addAllItems(cart.values().stream().map(this::convert).collect(Collectors.toList()))
        .build();
  }

  @SnapshotHandler
  public void handleSnapshot(ShoppingCartDomain.Cart cart) {
    this.cart.clear();
    for (ShoppingCartDomain.LineItem item : cart.getItemsList()) {
      this.cart.put(item.getProductId(), convert(item));
    }
  }

  @EventHandler
  public void itemAdded(ShoppingCartDomain.ItemAdded itemAdded) {
    ShoppingCartApi.LineItem item = cart.get(itemAdded.getItem().getProductId());
    if (item == null) {
      item = convert(itemAdded.getItem());
    } else {
      item =
          item.toBuilder()
              .setQuantity(item.getQuantity() + itemAdded.getItem().getQuantity())
              .build();
    }
    cart.put(item.getProductId(), item);
  }

  @EventHandler
  public void itemRemoved(ShoppingCartDomain.ItemRemoved itemRemoved) {
    cart.remove(itemRemoved.getProductId());
  }

  @CommandHandler
  public ShoppingCartApi.Cart getCart() {
    return ShoppingCartApi.Cart.newBuilder().addAllItems(cart.values()).build();
  }

  @CommandHandler
  public Empty addItem(ShoppingCartApi.AddLineItem item, CommandContext ctx) {
    if (item.getQuantity() <= 0) {
      throw ctx.fail("Cannot add negative quantity of to item" + item.getProductId());
    }
    ctx.emit(
        ShoppingCartDomain.ItemAdded.newBuilder()
            .setItem(
                ShoppingCartDomain.LineItem.newBuilder()
                    .setProductId(item.getProductId())
                    .setName(item.getName())
                    .setQuantity(item.getQuantity())
                    .build())
            .build());
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty removeItem(ShoppingCartApi.RemoveLineItem item, CommandContext ctx) {
    if (!cart.containsKey(item.getProductId())) {
      throw ctx.fail(
          "Cannot remove item " + item.getProductId() + " because it is not in the cart.");
    }
    ctx.emit(ShoppingCartDomain.ItemRemoved.newBuilder().setProductId(item.getProductId()).build());
    return Empty.getDefaultInstance();
  }

  private ShoppingCartApi.LineItem convert(ShoppingCartDomain.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }

  private ShoppingCartDomain.LineItem convert(ShoppingCartApi.LineItem item) {
    return ShoppingCartDomain.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
}
