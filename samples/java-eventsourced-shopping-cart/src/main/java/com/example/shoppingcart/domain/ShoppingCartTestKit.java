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

    // public ShoppingCartTestKit(String entityId){
    //     this.state = ShoppingCartDomain.Cart.newBuilder().build();
    //     this.entity = new ShoppingCart(entityId);
    // }


    //To bring some external state or limitations or context along with the entity
    public ShoppingCartTestKit(ShoppingCart entity){
        this.state = ShoppingCartDomain.Cart.newBuilder().build();
        this.entity = entity;
    }

    public ShoppingCartTestKit(String entityId, ShoppingCartDomain.Cart state){
        this.state = state;
        this.entity = new ShoppingCart(entityId);
    }

    public ShoppingCartDomain.Cart getState(){
            return state;
    }

    public List<Object> getAllEvents(){
        return this.events;
    }

    
    private List<Object> getEvents(EventSourcedEntityBase.Effect<Empty> effect){
        return CollectionConverters.asJava(helper.getEvents(effect));
    }

    // WIP - dealing with different replies. Forward, Error maybe even no reply
    private <Reply> Reply getReplyOfType(EventSourcedEntityBase.Effect<Empty> effect, ShoppingCartDomain.Cart state, Class<Reply> expectedClass){
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

    public Result<Empty> addItem(ShoppingCartApi.AddLineItem command) {
        //handling command
        EventSourcedEntityBase.Effect<Empty> effect = entity.addItem(state, command);
        List<Object> events = getEvents(effect); 
        this.events.add(events);
        //handling events
        for(Object e: events){
            this.state = handleEvent(state,e);
        }
        //reply
        //I'm assuming there is always and at least a single reply. Q is a correct assumption? 
        Empty reply = getReplyOfType(effect, this.state, Empty.class);
        return new Result(reply, CollectionConverters.asScala(events));
    };


    public Result<Empty> removeItem(ShoppingCartApi.RemoveLineItem command) {
        //TODO
        throw new java.lang.UnsupportedOperationException();
    };

    public Result<ShoppingCartApi.Cart> getCart(ShoppingCartApi.GetShoppingCart command) {
        //TODO
        throw new java.lang.UnsupportedOperationException();
    };
   
}
