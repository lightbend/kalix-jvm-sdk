/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.shoppingcart.domain;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;
import org.junit.Test;
import java.util.NoSuchElementException;
import org.mockito.*;
import com.example.shoppingcart.domain.ShoppingCartTestKit;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect;
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
import com.example.shoppingcart.domain.Result;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;

public class ShoppingCartTest {
    private String entityId = "entityId1";
    private ShoppingCart entity;

    @Test
    public void addItemTest() {

        // GIVEN
        ShoppingCartDomain.Cart initialState = ShoppingCartDomain.Cart.newBuilder().build();
        ShoppingCartApi.AddLineItem command = ShoppingCartApi.AddLineItem.newBuilder().setProductId("id1")
                .setName("name").setQuantity(2).build();

        //before and after, clean?  (not yet)
        ShoppingCartTestKit testKit = new ShoppingCartTestKit(entityId);

        //WHEN 
        Result<Empty> result = testKit.addItem(command);

        //THEN
        ShoppingCartDomain.LineItem addedLineItem = ShoppingCartDomain.LineItem.newBuilder().setProductId(command.getProductId())
                        .setName(command.getName()).setQuantity(command.getQuantity()).build();
        ShoppingCartDomain.ItemAdded expectedEvent = ShoppingCartDomain.ItemAdded.newBuilder()
                .setItem(addedLineItem)
                .build();

        ShoppingCartDomain.Cart expectedState = ShoppingCartDomain.Cart.newBuilder().addItems(addedLineItem).build();

        assertEquals(1, result.getEvents().size());
        assertEquals(1, testKit.getAllEvents().size());
        assertEquals(expectedEvent, result.getEventOfType(ShoppingCartDomain.ItemAdded.class));
        assertThrows(NoSuchElementException.class, () ->  result.getEventOfType(ShoppingCartDomain.ItemAdded.class));
        assertEquals(Empty.getDefaultInstance(), result.getReply());
        assertEquals(expectedState, testKit.getState());
    }
}
