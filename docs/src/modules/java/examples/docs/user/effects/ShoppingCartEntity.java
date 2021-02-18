/*
 * Copyright 2019 Lightbend Inc.
 */

package docs.user.effects;

import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.eventsourced.CommandContext;
import com.akkaserverless.javasdk.eventsourced.CommandHandler;
import com.example.Hotitems;
import com.example.Shoppingcart;
import com.google.protobuf.Empty;

public class ShoppingCartEntity {

  // tag::lookup[]
  private final ServiceCallRef<Hotitems.Item> itemAddedToCartRef;

  public ShoppingCartEntity(Context ctx) {
    itemAddedToCartRef =
        ctx.serviceCallFactory()
            .lookup(
                "example.shoppingcart.ShoppingCartService", "ItemAddedToCart", Hotitems.Item.class);
  }
  // end::lookup[]

  class CommandHandlerWithForward {
    // tag::forward[]
    @CommandHandler
    public void addItem(Shoppingcart.AddLineItem item, CommandContext ctx) {
      // ... Validate and emit event

      ctx.forward(
          itemAddedToCartRef.createCall(
              Hotitems.Item.newBuilder()
                  .setProductId(item.getProductId())
                  .setName(item.getName())
                  .setQuantity(item.getQuantity())
                  .build()));
    }
    // end::forward[]
  }

  class CommandHandlerWithEffect {
    // tag::effect[]
    @CommandHandler
    public Empty addItem(Shoppingcart.AddLineItem item, CommandContext ctx) {
      // ... Validate and emit event

      ctx.effect(
          itemAddedToCartRef.createCall(
              Hotitems.Item.newBuilder()
                  .setProductId(item.getProductId())
                  .setName(item.getName())
                  .setQuantity(item.getQuantity())
                  .build()));

      return Empty.getDefaultInstance();
    }
    // end::effect[]
  }
}
