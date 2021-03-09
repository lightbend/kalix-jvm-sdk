/*
 * Copyright 2019 Lightbend Inc.
 */

package docs.user.eventsourced;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.eventsourced.*;
import com.example.Domain;
import com.example.Shoppingcart;
import com.google.protobuf.Empty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// tag::entity-class[]
@EventSourcedEntity(entityType = "shopping-cart", snapshotEvery = 20)
public class ShoppingCartEntity {
  // end::entity-class[]

  // tag::entity-state[]
  private final Map<String, Shoppingcart.LineItem> cart = new LinkedHashMap<>();
  // end::entity-state[]

  // tag::constructing[]
  private final String entityId;

  public ShoppingCartEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }
  // end::constructing[]

  // tag::get-cart[]
  @CommandHandler
  public Shoppingcart.Cart getCart() {
    return Shoppingcart.Cart.newBuilder().addAllItems(cart.values()).build();
  }
  // end::get-cart[]

  // tag::add-item[]
  @CommandHandler
  public Empty addItem(Shoppingcart.AddLineItem item, CommandContext ctx) {
    if (item.getQuantity() <= 0) {
      ctx.fail("Cannot add negative quantity of to item" + item.getProductId());
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
  // end::add-item[]

  // tag::item-added[]
  @EventHandler
  public void itemAdded(Domain.ItemAdded itemAdded) {
    Shoppingcart.LineItem item = cart.get(itemAdded.getItem().getProductId());
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

  private Shoppingcart.LineItem convert(Domain.LineItem item) {
    return Shoppingcart.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
  // end::item-added[]

  // tag::snapshot[]
  @Snapshot
  public Domain.Cart snapshot() {
    return Domain.Cart.newBuilder()
        .addAllItems(cart.values().stream().map(this::convert).collect(Collectors.toList()))
        .build();
  }

  private Domain.LineItem convert(Shoppingcart.LineItem item) {
    return Domain.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
  // end::snapshot[]

  // tag::handle-snapshot[]
  @SnapshotHandler
  public void handleSnapshot(Domain.Cart cart) {
    this.cart.clear();
    for (Domain.LineItem item : cart.getItemsList()) {
      this.cart.put(item.getProductId(), convert(item));
    }
  }
  // end::handle-snapshot[]

  // tag::register[]
  public static void main(String... args) {
    new AkkaServerless()
        .registerEventSourcedEntity(
            ShoppingCartEntity.class,
            Shoppingcart.getDescriptor().findServiceByName("ShoppingCartService"),
            Domain.getDescriptor())
        .start();
  }
  // end::register[]

}
