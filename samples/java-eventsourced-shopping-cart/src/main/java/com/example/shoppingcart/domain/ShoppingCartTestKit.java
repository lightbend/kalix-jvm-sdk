package com.example.shoppingcart;

import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;
import java.util.ArrayList;
import java.util.List;

// Q Would we need ShoppingCartImpl as parameter constructor? 
public class ShoppingCartTestKit {

    ShoppingCartDomain.Cart currentState = ShoppingCartDomain.Cart.newBuilder().build();

    // Q What about having many types of events?
    public Result<ShoppingCartDomain.ItemAdded> addItem(ShoppingCartApi.AddLineItem command) {
        // TODO >> change this fake for actual call
        ShoppingCartDomain.ItemAdded expectedEvent = ShoppingCartDomain.ItemAdded.newBuilder()
                .setItem(ShoppingCartDomain.LineItem.newBuilder().setProductId(command.getProductId())
                        .setName(command.getName()).setQuantity(command.getQuantity()).build())
                .build();
        // << TODO
        List<ShoppingCartDomain.ItemAdded> events = new ArrayList<ShoppingCartDomain.ItemAdded>();
        events.add(expectedEvent);
        return new Result(Empty.getDefaultInstance(), events, currentState);
    };

    public Result<ShoppingCartDomain.ItemRemoved> removeItem(ShoppingCartApi.RemoveLineItem command) {
        return new Result(Empty.getDefaultInstance(), new ArrayList<ShoppingCartDomain.ItemRemoved>(), currentState);
    };

    // Q or get state?
    // Q what about the events in this one? what type?
    public Result getCart(ShoppingCartApi.GetShoppingCart command) {
        return new Result(Empty.getDefaultInstance(), new ArrayList<ShoppingCartDomain.ItemRemoved>(), currentState);
    };

    public final class Result<Event> {

        Result(Empty reply, List<Event> events, ShoppingCartDomain.Cart state) {
            this.reply = reply;
            this.events = events;
            this.state = state;
        }

        Empty reply;
        List<Event> events = new ArrayList<Event>();
        ShoppingCartDomain.Cart state;

        public Empty getReply() {
            return reply;
        }

        public List<Event> getEvents() {
            return events;
        }

        public ShoppingCartDomain.Cart getState() {
            return state;
        }
    }

}
