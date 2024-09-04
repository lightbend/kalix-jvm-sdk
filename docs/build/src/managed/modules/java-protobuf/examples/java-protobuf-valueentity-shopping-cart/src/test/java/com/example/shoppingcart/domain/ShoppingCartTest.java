/* This code was initialised by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.shoppingcart.domain;

import kalix.javasdk.testkit.ValueEntityResult;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShoppingCartTest {

    @Test
    public void createTest() {
        ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);

        ValueEntityResult<Empty> creation1Result = testKit.create(ShoppingCartApi.CreateCart.newBuilder().build());
        assertEquals(Empty.getDefaultInstance(), creation1Result.getReply());
        assertTrue(testKit.getState().getCreationTimestamp() > 0L);

        // creating an already created cart is not allowed
        ValueEntityResult<Empty> creation2Result = testKit.create(ShoppingCartApi.CreateCart.newBuilder().build());
        assertTrue(creation2Result.isError());
    }

    @Test
    public void addItemTest() {
        ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);

        ValueEntityResult<Empty> creation1Result = testKit.create(ShoppingCartApi.CreateCart.newBuilder().build());
        assertEquals(Empty.getDefaultInstance(), creation1Result.getReply());
        long creationTimeStamp = testKit.getState().getCreationTimestamp();

        ShoppingCartApi.AddLineItem commandA = ShoppingCartApi.AddLineItem.newBuilder().setProductId("idA")
                .setName("nameA").setQuantity(1).build();
        ValueEntityResult<Empty> resultA = testKit.addItem(commandA);
        assertEquals(Empty.getDefaultInstance(), resultA.getReply());
        assertTrue(resultA.stateWasUpdated());

        ShoppingCartApi.AddLineItem commandB = ShoppingCartApi.AddLineItem.newBuilder().setProductId("idB")
                .setName("nameB").setQuantity(2).build();
        ValueEntityResult<Empty> resultB = testKit.addItem(commandB);
        assertEquals(Empty.getDefaultInstance(), resultB.getReply());

        ShoppingCartDomain.LineItem expectedLineItemA = ShoppingCartDomain.LineItem.newBuilder().setProductId("idA")
                .setName("nameA").setQuantity(1).build();
        ShoppingCartDomain.LineItem expectedLineItemB = ShoppingCartDomain.LineItem.newBuilder().setProductId("idB")
                .setName("nameB").setQuantity(2).build();
        ShoppingCartDomain.Cart expectedState = ShoppingCartDomain.Cart.newBuilder()
                .setCreationTimestamp(creationTimeStamp)
                .addItems(expectedLineItemA)
                .addItems(expectedLineItemB)
                .build();
        assertEquals(expectedState, testKit.getState());
    }
}
