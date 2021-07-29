package com.example.shoppingcart.domain;

import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCart;
import com.google.protobuf.Empty;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import scala.jdk.javaapi.CollectionConverters;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase;
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
import com.akkaserverless.javasdk.testkit.AkkaserverlessTestKit;
import com.akkaserverless.javasdk.testkit.Result;

public class ShoppingCartTestKit {
    
    private ShoppingCartDomain.Cart state;
    private ShoppingCart entity;
    private List<Object> events = new ArrayList<Object>();
    private AkkaserverlessTestKit helper = new AkkaserverlessTestKit<ShoppingCartDomain.Cart>();

    //We might need to get rid of this one
    // Q - how can be sure an String will be enough to build the entity?
    public ShoppingCartTestKit(String entityId){
        this.state = ShoppingCartDomain.Cart.newBuilder().build();
        this.entity = new ShoppingCart(entityId);
    }

    //To bring some external state or context included in the entity constructor
    public ShoppingCartTestKit(ShoppingCart entity){
        this.state = ShoppingCartDomain.Cart.newBuilder().build();
        this.entity = entity;
    }

    public ShoppingCartTestKit(ShoppingCart entity, ShoppingCartDomain.Cart state){
        this.state = state;
        this.entity = entity;
    }

    public ShoppingCartDomain.Cart getState(){
            return state;
    }

    public List<Object> getAllEvents(){
        return this.events;
    }

    
    private <Reply> List<Object> getEvents(EventSourcedEntityBase.Effect<Reply> effect){
        return CollectionConverters.asJava(helper.getEvents(effect));
    }

    // WIP - dealing with different replies. Forward, Error maybe even no reply
    private <Reply> Reply getReplyOfType(EventSourcedEntityBase.Effect<Reply> effect, ShoppingCartDomain.Cart state){
        return (Reply) helper.getReply(effect, state);
    }

    //This should be using ShoppingCartHandler (codegen created. follow up with @raboof)
    private ShoppingCartDomain.Cart handleEvent(ShoppingCartDomain.Cart state, Object event) {
        if (event instanceof ShoppingCartDomain.ItemAdded) {
            return entity.itemAdded(state, (ShoppingCartDomain.ItemAdded) event);
        } else if (event instanceof ShoppingCartDomain.ItemRemoved) {
            return entity.itemRemoved(state, (ShoppingCartDomain.ItemRemoved) event);
        } else {
            throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
        }
    }

    private <Reply> Result<Reply> handleCommand(EventSourcedEntityBase.Effect<Reply> effect){
        List<Object> events = getEvents(effect); 
        this.events.add(events);
        for(Object e: events){
            this.state = handleEvent(state,e);
        }
        Reply reply = this.<Reply>getReplyOfType(effect, this.state);
        return new Result(reply, CollectionConverters.asScala(events));
    }

    public Result<Empty> addItem(ShoppingCartApi.AddLineItem command) {
        EventSourcedEntityBase.Effect<Empty> effect = entity.addItem(state, command);
        return handleCommand(effect);
    };

    public Result<Empty> removeItem(ShoppingCartApi.RemoveLineItem command) {
        EventSourcedEntityBase.Effect<Empty> effect = entity.removeItem(state, command);
        return handleCommand(effect);
    };

    public Result<ShoppingCartApi.Cart> getCart(ShoppingCartApi.GetShoppingCart command) {
        EventSourcedEntityBase.Effect<ShoppingCartApi.Cart> effect = entity.getCart(state, command);
        return handleCommand(effect);
    };
   
}
