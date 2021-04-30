/*
 * Copyright 2019 Lightbend Inc.
 */

package eventsourced.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.testkit.junit.jupiter.AkkaServerlessDescriptor;
import com.akkaserverless.javasdk.testkit.junit.jupiter.AkkaServerlessTest;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.ShoppingCartServiceClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@AkkaServerlessTest
class ShoppingCartIntegrationTest {

  @AkkaServerlessDescriptor private static final AkkaServerless SERVICE = Main.SERVICE;

  private final ShoppingCartServiceClient client;

  public ShoppingCartIntegrationTest(ShoppingCartServiceClient client) {
    this.client = client;
  }

  ShoppingCartApi.Cart getCart(String cartId) throws Exception {
    return client
        .getCart(ShoppingCartApi.GetShoppingCart.newBuilder().setCartId(cartId).build())
        .toCompletableFuture()
        .get();
  }

  void addItem(String cartId, String productId, String name, int quantity) throws Exception {
    client
        .addItem(
            ShoppingCartApi.AddLineItem.newBuilder()
                .setCartId(cartId)
                .setProductId(productId)
                .setName(name)
                .setQuantity(quantity)
                .build())
        .toCompletableFuture()
        .get();
  }

  void removeItem(String cartId, String productId) throws Exception {
    client
        .removeItem(
            ShoppingCartApi.RemoveLineItem.newBuilder()
                .setCartId(cartId)
                .setProductId(productId)
                .build())
        .toCompletableFuture()
        .get();
  }

  ShoppingCartApi.LineItem item(String productId, String name, int quantity) {
    return ShoppingCartApi.LineItem.newBuilder()
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
    addItem("cart2", "a", "Apple", 1);
    addItem("cart2", "b", "Banana", 2);
    addItem("cart2", "c", "Cantaloupe", 3);
    ShoppingCartApi.Cart cart = getCart("cart2");
    assertEquals(3, cart.getItemsCount(), "shopping cart should have 3 items");
    assertIterableEquals(
        cart.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)),
        "shopping cart should have expected items");
  }

  @Test
  void removeItemsFromCart() throws Exception {
    addItem("cart3", "a", "Apple", 1);
    addItem("cart3", "b", "Banana", 2);
    ShoppingCartApi.Cart cart1 = getCart("cart3");
    assertEquals(2, cart1.getItemsCount(), "shopping cart should have 2 items");
    assertIterableEquals(
        cart1.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2)),
        "shopping cart should have expected items");
    removeItem("cart3", "a");
    ShoppingCartApi.Cart cart2 = getCart("cart3");
    assertEquals(1, cart2.getItemsCount(), "shopping cart should have 1 item");
    assertIterableEquals(
        cart2.getItemsList(),
        List.of(item("b", "Banana", 2)),
        "shopping cart should have expected items");
  }
}
