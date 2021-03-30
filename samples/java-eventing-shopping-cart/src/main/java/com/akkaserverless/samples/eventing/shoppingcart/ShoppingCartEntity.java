/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.google.protobuf.Empty;
import shopping.cart.api.ShoppingCartApi;
import shopping.cart.model.ShoppingCart;

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
  public ShoppingCart.CartState snapshot() {
    return ShoppingCart.CartState.newBuilder()
        .addAllItems(cart.values().stream().map(this::convert).collect(Collectors.toList()))
        .build();
  }

  @SnapshotHandler
  public void handleSnapshot(ShoppingCart.CartState cart) {
    this.cart.clear();
    for (ShoppingCart.LineItem item : cart.getItemsList()) {
      this.cart.put(item.getProductId(), convert(item));
    }
  }

  @EventHandler
  public void itemAdded(ShoppingCart.ItemAdded itemAdded) {
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
  public void itemRemoved(ShoppingCart.ItemRemoved itemRemoved) {
    ShoppingCartApi.LineItem lineItem = cart.get(itemRemoved.getProductId());
    int newQty = lineItem.getQuantity() - itemRemoved.getQuantity();

    if (newQty > 0) {
      ShoppingCartApi.LineItem newItemLine = lineItem.toBuilder().setQuantity(newQty).build();
      cart.put(itemRemoved.getProductId(), newItemLine);
    } else {
      cart.remove(itemRemoved.getProductId());
    }
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
        ShoppingCart.ItemAdded.newBuilder()
            .setItem(
                ShoppingCart.LineItem.newBuilder()
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
    } else {
      ShoppingCartApi.LineItem lineItem = cart.get(item.getProductId());
      ShoppingCart.ItemRemoved event = null;
      if ((lineItem.getQuantity() - item.getQuantity()) > 0) {
        event =
            ShoppingCart.ItemRemoved.newBuilder()
                .setProductId(item.getProductId())
                .setQuantity(item.getQuantity()) // only remove requested quantity
                .build();
      } else {
        event =
            ShoppingCart.ItemRemoved.newBuilder()
                .setProductId(item.getProductId())
                .setQuantity(lineItem.getQuantity()) // remove all
                .build();
      }
      ctx.emit(event);
    }
    return Empty.getDefaultInstance();
  }

  private ShoppingCartApi.LineItem convert(ShoppingCart.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }

  private ShoppingCart.LineItem convert(ShoppingCartApi.LineItem item) {
    return ShoppingCart.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
}
