/*
 * Copyright 2019 Lightbend Inc.
 */

package valueentity.shoppingcart;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import com.example.valueentity.shoppingcart.ShoppingCartApi;
import com.example.valueentity.shoppingcart.ShoppingCartServiceClient;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ShoppingCartIntegrationTest {

  @ClassRule
  public static final AkkaServerlessTestkitResource testkit =
      new AkkaServerlessTestkitResource(Main.SERVICE);

  private final ShoppingCartServiceClient client;

  public ShoppingCartIntegrationTest() {
    this.client =
        ShoppingCartServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
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

  void removeCart(String cartId) throws Exception {
    client
        .removeCart(ShoppingCartApi.RemoveShoppingCart.newBuilder().setCartId(cartId).build())
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
  public void emptyCartByDefault() throws Exception {
    assertEquals("shopping cart should be empty", 0, getCart("user1").getItemsCount());
  }

  @Test
  public void addItemsToCart() throws Exception {
    addItem("cart2", "a", "Apple", 1);
    addItem("cart2", "b", "Banana", 2);
    addItem("cart2", "c", "Cantaloupe", 3);
    ShoppingCartApi.Cart cart = getCart("cart2");
    assertEquals("shopping cart should have 3 items", 3, cart.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)));
  }

  @Test
  public void removeItemsFromCart() throws Exception {
    addItem("cart3", "a", "Apple", 1);
    addItem("cart3", "b", "Banana", 2);
    ShoppingCartApi.Cart cart1 = getCart("cart3");
    assertEquals("shopping cart should have 2 items", 2, cart1.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart1.getItemsList(),
        List.of(item("a", "Apple", 1), item("b", "Banana", 2)));
    removeItem("cart3", "a");
    ShoppingCartApi.Cart cart2 = getCart("cart3");
    assertEquals("shopping cart should have 1 item", 1, cart2.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart2.getItemsList(),
        List.of(item("b", "Banana", 2)));
  }

  @Test
  public void removeCart() throws Exception {
    addItem("cart4", "a", "Apple", 42);
    ShoppingCartApi.Cart cart1 = getCart("cart4");
    assertEquals("shopping cart should have 1 item", 1, cart1.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart1.getItemsList(),
        List.of(item("a", "Apple", 42)));
    removeCart("cart4");
    assertEquals("shopping cart should be empty", 0, getCart("cart4").getItemsCount());
  }
}
