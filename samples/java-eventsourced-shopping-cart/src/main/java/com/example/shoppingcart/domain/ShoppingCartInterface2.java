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
    
    public class CommandNotImplementedException extends UnsupportedOperationException {
        public CommandNotImplementedException() {
            super("You have either created a new command or removed the handling of an existing command. Please declare a method in your \"impl\" class for this command.");
        }
    }

    protected abstract ShoppingCartDomain.Cart emptyState();

    @CommandHandler(name = "AddItem")
    public abstract Effect<Empty> addItem(
            ShoppingCartApi.AddLineItem command,
            ShoppingCartDomain.Cart currentState,
            EventSourcedEntityEffect.Builder<Empty, ShoppingCartDomain.Cart> effectBuilder,
            CommandContext ctx);

    @CommandHandler(name = "RemoveItem")
    public abstract Effect<Empty> removeItem(
            ShoppingCartApi.RemoveLineItem command,
            ShoppingCartDomain.Cart currentState,
            EventSourcedEntityEffect.Builder<Empty, ShoppingCartDomain.Cart> effectBuilder,
            CommandContext ctx);

    @CommandHandler(name = "GetCart")
    public abstract Effect<ShoppingCartApi.Cart> getCart(
            ShoppingCartApi.GetShoppingCart command,
            ShoppingCartDomain.Cart currentState,
            EventSourcedEntityEffect.Builder<ShoppingCartApi.Cart, ShoppingCartDomain.Cart> effectBuilder,
            CommandContext ctx);

    @EventHandler
    protected abstract ShoppingCartDomain.Cart itemAdded(ShoppingCartDomain.ItemAdded event, ShoppingCartDomain.Cart currentState);
    
    @EventHandler
    protected abstract ShoppingCartDomain.Cart itemRemoved(ShoppingCartDomain.ItemRemoved event, ShoppingCartDomain.Cart currentState);
}
