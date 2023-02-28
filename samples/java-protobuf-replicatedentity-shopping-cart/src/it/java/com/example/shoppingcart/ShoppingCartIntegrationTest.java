package com.example.shoppingcart;

import kalix.javasdk.testkit.junit.KalixTestKitResource;
import com.example.shoppingcart.Main;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.ShoppingCartService;
import io.grpc.StatusRuntimeException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.instanceOf;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class ShoppingCartIntegrationTest {

  /** The test kit starts both the service container and the Kalix proxy. */
  @ClassRule
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(Main.createKalix());

  /** Use the generated gRPC client to call the service through the Kalix proxy. */
  private final ShoppingCartService client;

  public ShoppingCartIntegrationTest() {
    client = testKit.getGrpcClient(ShoppingCartService.class);
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

  void removeItem(String cartId, String productId, String name) throws Exception {
    client
        .removeItem(
            ShoppingCartApi.RemoveLineItem.newBuilder()
                .setCartId(cartId)
                .setProductId(productId)
                .setName(name)
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
    removeItem("cart3", "a", "Apple");
    ShoppingCartApi.Cart cart2 = getCart("cart3");
    assertEquals("shopping cart should have 1 item", 1, cart2.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        cart2.getItemsList(),
        List.of(item("b", "Banana", 2)));
  }

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

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
    exceptionRule.expect(ExecutionException.class);
    exceptionRule.expectMessage("INTERNAL: Entity deleted");
    exceptionRule.expectCause(instanceOf(StatusRuntimeException.class));
    getCart("cart4");
  }
}
