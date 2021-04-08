/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.shoppingcart;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import com.example.valueentity.shoppingcart.ShoppingCart;
import com.example.valueentity.shoppingcart.ShoppingCartServiceClient;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ShoppingCartIntegrationTest {

  @ClassRule
  public static final AkkaServerlessTestkitResource testkit =
      new AkkaServerlessTestkitResource(Main.shoppingCart);

  private final ShoppingCartServiceClient client;

  public ShoppingCartIntegrationTest() {
    this.client =
        ShoppingCartServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
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

  void removeCart(String userId) throws Exception {
    client
        .removeCart(ShoppingCart.RemoveShoppingCart.newBuilder().setUserId(userId).build())
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
  public void emptyCartByDefault() throws Exception {
    assertEquals("shopping cart should be empty", 0, getCart("user1").getItemsCount());
  }

  @Test
  public void addItemsToCart() throws Exception {
    addItem("user2", "a", "Apple", 1);
    addItem("user2", "b", "Banana", 2);
    addItem("user2", "c", "Cantaloupe", 3);
    ShoppingCart.Cart cart = getCart("user2");
    assertEquals("shopping cart should have 3 items", 3, cart.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)));
  }

  @Test
  public void removeItemsFromCart() throws Exception {
    addItem("user3", "a", "Apple", 1);
    addItem("user3", "b", "Banana", 2);
    ShoppingCart.Cart cart1 = getCart("user3");
    assertEquals("shopping cart should have 2 items", 2, cart1.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart1.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2)));
    removeItem("user3", "a");
    ShoppingCart.Cart cart2 = getCart("user3");
    assertEquals("shopping cart should have 1 item", 1, cart2.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart2.getItemsList(),
        List.of(item("b", "Banana", 2)));
  }

  @Test
  public void removeCart() throws Exception {
    addItem("user4", "a", "Apple", 42);
    ShoppingCart.Cart cart1 = getCart("user4");
    assertEquals("shopping cart should have 1 item", 1, cart1.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart1.getItemsList(),
        List.of(item("a", "Apple", 42)));
    removeCart("user4");
    assertEquals("shopping cart should be empty", 0, getCart("user4").getItemsCount());
  }
}
