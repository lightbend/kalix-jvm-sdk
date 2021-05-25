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

package shopping;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import shopping.cart.api.ShoppingCartApi;
import shopping.cart.api.ShoppingCartServiceClient;
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

  void removeItem(String cartId, String productId, int quantity) throws Exception {
    client
        .removeItem(
            ShoppingCartApi.RemoveLineItem.newBuilder()
                .setCartId(cartId)
                .setProductId(productId)
                .setQuantity(quantity)
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
  public void emptyCartByDefault() throws Exception {
    assertEquals("shopping cart should be empty", 0, getCart("user1").getItemsCount());
  }

  @Test
  public void addItemsToCart() throws Exception {
    final String cartId = "cart2";
    addItem(cartId, "a", "Apple", 1);
    addItem(cartId, "b", "Banana", 2);
    addItem(cartId, "c", "Cantaloupe", 3);
    ShoppingCartApi.Cart cart = getCart(cartId);
    assertEquals("shopping cart should have 3 items", 3, cart.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)),
        cart.getItemsList());
  }

  @Test
  public void removeItemsFromCart() throws Exception {
    final String cartId = "cart3";
    addItem(cartId, "a", "Apple", 1);
    addItem(cartId, "b", "Banana", 2);
    ShoppingCartApi.Cart cart1 = getCart(cartId);
    assertEquals("shopping cart should have 2 items", 2, cart1.getItemsCount());
    assertEquals(
        "shopping cart should have expected items",
        List.of(item("a", "Apple", 1), item("b", "Banana", 2)),
        cart1.getItemsList());
    removeItem(cartId, "a", 1);
    ShoppingCartApi.Cart cart2 = getCart(cartId);
    assertEquals(
        "shopping cart should have expected items after removal",
        List.of(item("b", "Banana", 2)),
        cart2.getItemsList());
    assertEquals(
        "shopping cart should have expected items",
        cart2.getItemsList(),
        List.of(item("b", "Banana", 2)));
  }
}
