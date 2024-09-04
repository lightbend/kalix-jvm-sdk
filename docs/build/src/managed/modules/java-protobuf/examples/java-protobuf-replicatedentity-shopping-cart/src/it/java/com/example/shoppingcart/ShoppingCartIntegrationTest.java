package com.example.shoppingcart;

import io.grpc.StatusRuntimeException;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class ShoppingCartIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix Runtime.
   */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
    new KalixTestKitExtension(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix Runtime.
   */
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
    assertEquals(0, getCart("user1").getItemsCount(), "shopping cart should be empty");
  }

  @Test
  public void addItemsToCart() throws Exception {
    addItem("cart2", "a", "Apple", 1);
    addItem("cart2", "b", "Banana", 2);
    addItem("cart2", "c", "Cantaloupe", 3);
    ShoppingCartApi.Cart cart = getCart("cart2");
    assertEquals(3, cart.getItemsCount(), "shopping cart should have 3 items");
    assertEquals(
      List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)),
      cart.getItemsList(),
      "shopping cart should have expected items");
  }

  @Test
  public void removeItemsFromCart() throws Exception {
    addItem("cart3", "a", "Apple", 1);
    addItem("cart3", "b", "Banana", 2);
    ShoppingCartApi.Cart cart1 = getCart("cart3");
    assertEquals(2, cart1.getItemsCount(), "shopping cart should have 2 items");
    assertEquals(
      List.of(item("a", "Apple", 1), item("b", "Banana", 2)),
      cart1.getItemsList(),
      "shopping cart should have expected items");
    removeItem("cart3", "a", "Apple");
    ShoppingCartApi.Cart cart2 = getCart("cart3");
    assertEquals(1, cart2.getItemsCount(), "shopping cart should have 1 item");
    assertEquals(
      List.of(item("b", "Banana", 2)),
      cart2.getItemsList(),
      "shopping cart should have expected items");
  }


  @Test
  public void removeCart() throws Exception {
    addItem("cart4", "a", "Apple", 42);
    ShoppingCartApi.Cart cart1 = getCart("cart4");
    assertEquals(1, cart1.getItemsCount(), "shopping cart should have 1 item");
    assertEquals(
      List.of(item("a", "Apple", 42)),
      cart1.getItemsList(),
      "shopping cart should have expected items");
    removeCart("cart4");

    var cause =
      Assertions.assertThrows(
        ExecutionException.class,
        () -> getCart("cart4"),
        "INTERNAL: Entity deleted").getCause();
    instanceOf(StatusRuntimeException.class).matches(cause);

  }
}
