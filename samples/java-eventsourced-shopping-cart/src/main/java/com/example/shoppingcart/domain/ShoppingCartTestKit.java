package com.example.shoppingcart;

import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import com.akkaserverless.javasdk.testkit.Result;

// Q Would we need ShoppingCartImpl as parameter constructor? 
public class ShoppingCartTestKit {
    
    ShoppingCartDomain.Cart state;

    public ShoppingCartTestKit(ShoppingCartDomain.Cart state){
        this.state = state;
    }

    // Q - better have the state outside always accessible
    //     than adding another field to Result
    public ShoppingCartDomain.Cart getState(){
            return state;
    }

    public Result<Empty> addItem(ShoppingCartApi.AddLineItem command) {
        // TODO >> change this fake for actual call
        ShoppingCartDomain.LineItem addedLineItem = ShoppingCartDomain.LineItem.newBuilder().setProductId(command.getProductId())
                        .setName(command.getName()).setQuantity(command.getQuantity()).build();
        ShoppingCartDomain.ItemAdded expectedEvent = ShoppingCartDomain.ItemAdded.newBuilder()
                .setItem(addedLineItem)
                .build();
        
        Queue<ShoppingCartDomain.ItemAdded> events = new LinkedList<ShoppingCartDomain.ItemAdded>();
        events.add(expectedEvent);
        this.state = state.newBuilder().addItems(addedLineItem).build();
        // << TODO
        return new Result(Empty.getDefaultInstance(), events);
    };

    public Result<Empty> removeItem(ShoppingCartApi.RemoveLineItem command) {
        return new Result(Empty.getDefaultInstance(), new LinkedList<ShoppingCartDomain.ItemRemoved>());
    };

    public Result<ShoppingCartApi.Cart> getCart(ShoppingCartApi.GetShoppingCart command) {
        return new Result(Empty.getDefaultInstance(), new LinkedList<ShoppingCartDomain.ItemRemoved>());
    };



    //move it out to generic
   
}
