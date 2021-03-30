/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventsourced.shoppingcart;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.example.shoppingcart.ShoppingCart;
import com.example.shoppingcart.persistence.Domain;
import com.google.protobuf.Empty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** An event sourced entity. */
@EventSourcedEntity(entityType = "eventsourced-shopping-cart")
public class ShoppingCartEntity {
  private final String entityId;
  private final Map<String, ShoppingCart.LineItem> cart = new LinkedHashMap<>();

  public ShoppingCartEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }

  @Snapshot
  public Domain.Cart snapshot() {
    return Domain.Cart.newBuilder()
        .addAllItems(cart.values().stream().map(this::convert).collect(Collectors.toList()))
        .build();
  }

  @SnapshotHandler
  public void handleSnapshot(Domain.Cart cart) {
    this.cart.clear();
    for (Domain.LineItem item : cart.getItemsList()) {
      this.cart.put(item.getProductId(), convert(item));
    }
  }

  @EventHandler
  public void itemAdded(Domain.ItemAdded itemAdded) {
    ShoppingCart.LineItem item = cart.get(itemAdded.getItem().getProductId());
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
  public void itemRemoved(Domain.ItemRemoved itemRemoved) {
    cart.remove(itemRemoved.getProductId());
  }

  @CommandHandler
  public ShoppingCart.Cart getCart() {
    return ShoppingCart.Cart.newBuilder().addAllItems(cart.values()).build();
  }

  @CommandHandler
  public Empty addItem(ShoppingCart.AddLineItem item, CommandContext ctx) {
    if (item.getQuantity() <= 0) {
      throw ctx.fail("Cannot add negative quantity of to item" + item.getProductId());
    }
    ctx.emit(
        Domain.ItemAdded.newBuilder()
            .setItem(
                Domain.LineItem.newBuilder()
                    .setProductId(item.getProductId())
                    .setName(item.getName())
                    .setQuantity(item.getQuantity())
                    .build())
            .build());
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty removeItem(ShoppingCart.RemoveLineItem item, CommandContext ctx) {
    if (!cart.containsKey(item.getProductId())) {
      throw ctx.fail(
          "Cannot remove item " + item.getProductId() + " because it is not in the cart.");
    }
    ctx.emit(Domain.ItemRemoved.newBuilder().setProductId(item.getProductId()).build());
    return Empty.getDefaultInstance();
  }

  private ShoppingCart.LineItem convert(Domain.LineItem item) {
    return ShoppingCart.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }

  private Domain.LineItem convert(ShoppingCart.LineItem item) {
    return Domain.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
}
