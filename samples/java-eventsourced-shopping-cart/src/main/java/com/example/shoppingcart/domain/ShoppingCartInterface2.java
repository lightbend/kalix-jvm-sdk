package com.example.shoppingcart.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityEffect;
import com.akkaserverless.javasdk.eventsourcedentity.Snapshot;
import com.akkaserverless.javasdk.eventsourcedentity.SnapshotHandler;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;

/** An event sourced entity. */
public abstract class ShoppingCartInterface2 {

    protected abstract ShoppingCartDomain.Cart emptyState();

    @CommandHandler(name = "AddItem")
    public abstract Effect<Empty> addItem(
            ShoppingCartDomain.Cart currentState,
            ShoppingCartApi.AddLineItem command,
            EventSourcedEntityEffect.Builder<Empty, ShoppingCartDomain.Cart> effectBuilder,
            CommandContext ctx);

    @CommandHandler(name = "RemoveItem")
    public abstract Effect<Empty> removeItem(
            ShoppingCartDomain.Cart currentState,
            ShoppingCartApi.RemoveLineItem command,
            EventSourcedEntityEffect.Builder<Empty, ShoppingCartDomain.Cart> effectBuilder,
            CommandContext ctx);

    @CommandHandler(name = "GetCart")
    public abstract Effect<ShoppingCartApi.Cart> getCart(
            ShoppingCartDomain.Cart currentState,
            ShoppingCartApi.GetShoppingCart command,
            EventSourcedEntityEffect.Builder<ShoppingCartApi.Cart, ShoppingCartDomain.Cart> effectBuilder,
            CommandContext ctx);

    @EventHandler
    protected abstract ShoppingCartDomain.Cart itemAdded(
            ShoppingCartDomain.Cart currentState,
            ShoppingCartDomain.ItemAdded event);
    
    @EventHandler
    protected abstract ShoppingCartDomain.Cart itemRemoved(
            ShoppingCartDomain.Cart currentState,
            ShoppingCartDomain.ItemRemoved event);
}
