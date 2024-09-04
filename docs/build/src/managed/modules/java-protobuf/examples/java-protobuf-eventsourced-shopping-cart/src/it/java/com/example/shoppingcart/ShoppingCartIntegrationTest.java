/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.shoppingcart;

// tag::sample-it-test[]

import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
// ...
// end::sample-it-test[]
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
// tag::sample-it-test[]

public class ShoppingCartIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix Runtime.
   */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
    new KalixTestKitExtension(Main.createKalix()); // <1>

  private final ShoppingCartService client;

  public ShoppingCartIntegrationTest() {
    this.client = testKit.getGrpcClient(ShoppingCartService.class); // <2>
  }

  // end::sample-it-test[]

  @Test
  public void emptyCartByDefault() throws Exception {
    assertEquals(0, getCart("user1").getItemsCount(), "shopping cart should be empty");
  }

  // tag::sample-it-test[]
  @Test
  public void addItemsToCart() throws Exception { // <3>
    addItem("cart2", "a", "Apple", 1);
    addItem("cart2", "b", "Banana", 2);
    addItem("cart2", "c", "Cantaloupe", 3);
    ShoppingCartApi.Cart cart = getCart("cart2");
    assertEquals(3, cart.getItemsCount(), "shopping cart should have 3 items");
    assertEquals(
      cart.getItemsList(),
      List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)),
      "shopping cart should have expected items"
    );
  }

  // end::sample-it-test[]

  @Test
  public void removeItemsFromCart() throws Exception {
    addItem("cart3", "a", "Apple", 1);
    addItem("cart3", "b", "Banana", 2);
    ShoppingCartApi.Cart cart1 = getCart("cart3");
    assertEquals(2, cart1.getItemsCount(), "shopping cart should have 2 items");
    assertEquals(
      cart1.getItemsList(),
      List.of(item("a", "Apple", 1), item("b", "Banana", 2)),
      "shopping cart should have expected items"

    );
    removeItem("cart3", "a");
    ShoppingCartApi.Cart cart2 = getCart("cart3");
    assertEquals(1, cart2.getItemsCount(), "shopping cart should have 1 item");
    assertEquals(
      cart2.getItemsList(),
      List.of(item("b", "Banana", 2)),
      "shopping cart should have expected items"
    );
  }

  // tag::sample-it-test[]
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

  // end::sample-it-test[]
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

  // tag::sample-it-test[]
  ShoppingCartApi.LineItem item(String productId, String name, int quantity) {
    return ShoppingCartApi.LineItem.newBuilder()
      .setProductId(productId)
      .setName(name)
      .setQuantity(quantity)
      .build();
  }
}
// end::sample-it-test[]
