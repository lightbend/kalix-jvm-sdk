/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.shoppingcart.domain;

import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;
import org.junit.Test;
import java.util.NoSuchElementException;
import com.example.shoppingcart.domain.ShoppingCartTestKit;
import com.akkaserverless.javasdk.testkit.Result;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;

public class ShoppingCartTest {

    @Test
    public void addItemTest() {
        ShoppingCartTestKit testKit = new ShoppingCartTestKit(new ShoppingCart());

        ShoppingCartApi.AddLineItem apples = ShoppingCartApi.AddLineItem.newBuilder().setProductId("idA")
                .setName("apples").setQuantity(1).build();
        Result<Empty> addingApplesResult = testKit.addItem(apples);

        ShoppingCartApi.AddLineItem bananas = ShoppingCartApi.AddLineItem.newBuilder().setProductId("idB")
                .setName("bananas").setQuantity(2).build();
        testKit.addItem(bananas);

        assertEquals(1, addingApplesResult.getAllEvents().size());
        assertEquals(2, testKit.getAllEvents().size());

        ShoppingCartDomain.ItemAdded addedApples = addingApplesResult.getNextEventOfType(ShoppingCartDomain.ItemAdded.class);
        assertEquals("apples", addedApples.getItem().getName());
        assertThrows(NoSuchElementException.class, () ->  addingApplesResult.getNextEventOfType(ShoppingCartDomain.ItemAdded.class));
        assertEquals(Empty.getDefaultInstance(), addingApplesResult.getReply());

        ShoppingCartDomain.LineItem expectedApples = ShoppingCartDomain.LineItem.newBuilder().setProductId("idA")
                .setName("apples").setQuantity(1).build();        
        ShoppingCartDomain.LineItem expectedBananas = ShoppingCartDomain.LineItem.newBuilder().setProductId("idB")
                .setName("bananas").setQuantity(2).build();
        ShoppingCartDomain.Cart expectedState = ShoppingCartDomain.Cart.newBuilder()
                .addItems(expectedApples)
                .addItems(expectedBananas)
                .build();
        assertEquals(expectedState, testKit.getState());
    }
}
