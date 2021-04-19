/*
 * Copyright 2019 Lightbend Inc.
 */

package docs.user.replicatedentity;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.replicatedentity.*;
import com.example.Shoppingcart;
import com.google.protobuf.Empty;

import java.util.Optional;

// tag::entity-class[]
@ReplicatedEntity
public class ShoppingCartEntity {
  // end::entity-class[]

  // tag::creation[]
  private final LWWRegisterMap<String, Shoppingcart.LineItem> items;

  public ShoppingCartEntity(LWWRegisterMap<String, Shoppingcart.LineItem> items) {
    this.items = items;
  }
  // end::creation[]

  // tag::get-cart[]
  @CommandHandler
  public Shoppingcart.Cart getCart() {
    return Shoppingcart.Cart.newBuilder().addAllItems(items.values()).build();
  }
  // end::get-cart[]

  // tag::add-item[]
  @CommandHandler
  public Empty addItem(Shoppingcart.AddLineItem item, CommandContext ctx) {
    if (item.getQuantity() <= 0) {
      throw ctx.fail("Cannot add a negative quantity of items.");
    }
    if (items.containsKey(item.getProductId())) {
      items.computeIfPresent(
          item.getProductId(),
          (id, old) -> old.toBuilder().setQuantity(old.getQuantity() + item.getQuantity()).build());
    } else {
      items.put(
          item.getProductId(),
          Shoppingcart.LineItem.newBuilder()
              .setProductId(item.getProductId())
              .setName(item.getName())
              .setQuantity(item.getQuantity())
              .build());
    }
    return Empty.getDefaultInstance();
  }
  // end::add-item[]

  // tag::watch-cart[]
  @CommandHandler
  public Shoppingcart.Cart watchCart(StreamedCommandContext<Shoppingcart.Cart> ctx) {

    ctx.onChange(subscription -> Optional.of(getCart()));

    return getCart();
  }
  // end::watch-cart[]

  // tag::register[]
  public static void main(String... args) {
    new AkkaServerless()
        .registerReplicatedEntity(
            ShoppingCartEntity.class,
            Shoppingcart.getDescriptor().findServiceByName("ShoppingCartService"))
        .start();
  }
  // end::register[]

}
