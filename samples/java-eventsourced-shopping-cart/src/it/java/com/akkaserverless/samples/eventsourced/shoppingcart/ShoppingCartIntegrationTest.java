/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventsourced.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.testkit.junit.jupiter.AkkaServerlessDescriptor;
import com.akkaserverless.javasdk.testkit.junit.jupiter.AkkaServerlessTest;
import com.example.shoppingcart.ShoppingCart;
import com.example.shoppingcart.ShoppingCartServiceClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@AkkaServerlessTest
class ShoppingCartIntegrationTest {

  @AkkaServerlessDescriptor private static final AkkaServerless SHOPPING_CART = Main.shoppingCart;

  private final ShoppingCartServiceClient client;

  public ShoppingCartIntegrationTest(ShoppingCartServiceClient client) {
    this.client = client;
  }

  ShoppingCart.Cart getCart(String userId) throws Exception {
    return client
        .getCart(ShoppingCart.GetShoppingCart.newBuilder().setUserId(userId).build())
        .toCompletableFuture()
        .get();
  }

  void addItem(String userId, String productId, String name, int quantity) throws Exception {
    client
        .addItem(
            ShoppingCart.AddLineItem.newBuilder()
                .setUserId(userId)
                .setProductId(productId)
                .setName(name)
                .setQuantity(quantity)
                .build())
        .toCompletableFuture()
        .get();
  }

  void removeItem(String userId, String productId) throws Exception {
    client
        .removeItem(
            ShoppingCart.RemoveLineItem.newBuilder()
                .setUserId(userId)
                .setProductId(productId)
                .build())
        .toCompletableFuture()
        .get();
  }

  ShoppingCart.LineItem item(String productId, String name, int quantity) {
    return ShoppingCart.LineItem.newBuilder()
        .setProductId(productId)
        .setName(name)
        .setQuantity(quantity)
        .build();
  }

  @Test
  void emptyCartByDefault() throws Exception {
    assertEquals(0, getCart("user1").getItemsCount(), "shopping cart should be empty");
  }

  @Test
  void addItemsToCart() throws Exception {
    addItem("user2", "a", "Apple", 1);
    addItem("user2", "b", "Banana", 2);
    addItem("user2", "c", "Cantaloupe", 3);
    ShoppingCart.Cart cart = getCart("user2");
    assertEquals(3, cart.getItemsCount(), "shopping cart should have 3 items");
    assertIterableEquals(
        cart.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)),
        "shopping cart should have expected items");
  }

  @Test
  void removeItemsFromCart() throws Exception {
    addItem("user3", "a", "Apple", 1);
    addItem("user3", "b", "Banana", 2);
    ShoppingCart.Cart cart1 = getCart("user3");
    assertEquals(2, cart1.getItemsCount(), "shopping cart should have 2 items");
    assertIterableEquals(
        cart1.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2)),
        "shopping cart should have expected items");
    removeItem("user3", "a");
    ShoppingCart.Cart cart2 = getCart("user3");
    assertEquals(1, cart2.getItemsCount(), "shopping cart should have 1 item");
    assertIterableEquals(
        cart2.getItemsList(),
        List.of(item("b", "Banana", 2)),
        "shopping cart should have expected items");
  }
}
