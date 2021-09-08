/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.shoppingcart.domain;

import com.akkaserverless.javasdk.ServiceCallFactory;
import com.akkaserverless.javasdk.testkit.ValueEntityResult;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShoppingCartTest {

    @Test
    public void addItemTest() {
        // FIXME avoid having to create this
        ValueEntityContext valueEntityCreationContext = new ValueEntityContext() {
            @Override
            public String entityId() {
                return "cart";
            }

            @Override
            public ServiceCallFactory serviceCallFactory() {
                throw new UnsupportedOperationException("not implemented yet");
            }
        };

        ShoppingCartTestKit testKit = new ShoppingCartTestKit(new ShoppingCart(valueEntityCreationContext));

        ShoppingCartApi.AddLineItem commandA = ShoppingCartApi.AddLineItem.newBuilder().setProductId("idA")
                .setName("nameA").setQuantity(1).build();
        ValueEntityResult<Empty> resultA = testKit.addItem(commandA);

        ShoppingCartApi.AddLineItem commandB = ShoppingCartApi.AddLineItem.newBuilder().setProductId("idB")
                .setName("nameB").setQuantity(2).build();
        testKit.addItem(commandB);
        assertEquals(Empty.getDefaultInstance(), resultA.getReply());

        ShoppingCartDomain.LineItem expectedLineItemA = ShoppingCartDomain.LineItem.newBuilder().setProductId("idA")
                .setName("nameA").setQuantity(1).build();        
        ShoppingCartDomain.LineItem expectedLineItemB = ShoppingCartDomain.LineItem.newBuilder().setProductId("idB")
                .setName("nameB").setQuantity(2).build();
        ShoppingCartDomain.Cart expectedState = ShoppingCartDomain.Cart.newBuilder()
                .addItems(expectedLineItemA)
                .addItems(expectedLineItemB)
                .build();
        assertEquals(expectedState, testKit.getState());
    }
}
