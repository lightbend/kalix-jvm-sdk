/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.shoppingcart.domain;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;
import org.junit.Test;
import org.mockito.*;
import com.example.shoppingcart.domain.EventSourcedTestKit;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect;
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;

//TODO change Testkit to avoid java conversions
import scala.jdk.javaapi.CollectionConverters;


import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;

public class ShoppingCartTest {
    private String entityId = "entityId1";
    private ShoppingCartImpl entity;
    private CommandContext context = Mockito.mock(CommandContext.class);
    
    @Test
    public void addItemTest() {
        entity = new ShoppingCartImpl(entityId);
        
        ShoppingCartDomain.Cart currentState = ShoppingCartDomain.Cart.newBuilder().build();
        ShoppingCartApi.AddLineItem command = ShoppingCartApi.AddLineItem.newBuilder()
            .setProductId("id1")
            .setName("name")
            .setQuantity(2)
            .build();
        
        //TODO passing params to abstract the Result (see below)
        EventSourcedTestKit eventSourcedTestKit = new EventSourcedTestKit();//.create(entity);

        ShoppingCartDomain.ItemAdded expectedEvent = ShoppingCartDomain.ItemAdded.newBuilder()
            .setItem(
                    ShoppingCartDomain.LineItem.newBuilder()
                            .setProductId(command.getProductId())
                            .setName(command.getName())
                            .setQuantity(command.getQuantity())
                            .build())
            .build();

        Result result = eventSourcedTestKit.runCommand("addItem",command,currentState);
        //TODO change Testkit to avoid java conversions
        assertEquals(expectedEvent, (ShoppingCartDomain.ItemAdded)CollectionConverters.asJava(result.events()).get(0));
        assertEquals(Empty.getDefaultInstance(),result.reply());
    }
}


