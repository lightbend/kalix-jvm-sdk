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
package com.example.shoppingcart.domain;

import kalix.javasdk.replicatedentity.ReplicatedCounterMap;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// tag::class[]
public class ShoppingCart extends AbstractShoppingCart { // <1>
  // end::class[]

  @SuppressWarnings("unused")
  private final String entityId;

  public ShoppingCart(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::addItem[]
  @Override
  public Effect<Empty> addItem(
      ReplicatedCounterMap<ShoppingCartDomain.Product> cart,
      ShoppingCartApi.AddLineItem addLineItem) {

    if (addLineItem.getQuantity() <= 0) { // <1>
      return effects().error("Quantity for item " + addLineItem.getProductId() + " must be greater than zero.");
    }

    ShoppingCartDomain.Product product = // <2>
        ShoppingCartDomain.Product.newBuilder()
            .setId(addLineItem.getProductId())
            .setName(addLineItem.getName())
            .build();

    ReplicatedCounterMap<ShoppingCartDomain.Product> updatedCart = // <3>
        cart.increment(product, addLineItem.getQuantity());

    return effects()
        .update(updatedCart) // <4>
        .thenReply(Empty.getDefaultInstance()); // <5>
  }
  // end::addItem[]

  @Override
  public Effect<Empty> removeItem(
      ReplicatedCounterMap<ShoppingCartDomain.Product> cart,
      ShoppingCartApi.RemoveLineItem removeLineItem) {

    ShoppingCartDomain.Product product =
        ShoppingCartDomain.Product.newBuilder()
            .setId(removeLineItem.getProductId())
            .setName(removeLineItem.getName())
            .build();

    if (!cart.containsKey(product)) {
      return effects().error("Item to remove is not in the cart: " + removeLineItem.getProductId());
    }

    return effects()
        .update(cart.remove(product))
        .thenReply(Empty.getDefaultInstance());
  }

  // tag::getCart[]
  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
      ReplicatedCounterMap<ShoppingCartDomain.Product> cart, // <1>
      ShoppingCartApi.GetShoppingCart getShoppingCart) {

    List<ShoppingCartApi.LineItem> allItems =
        cart.keySet().stream()
            .map(
                product ->
                    ShoppingCartApi.LineItem.newBuilder()
                        .setProductId(product.getId())
                        .setName(product.getName())
                        .setQuantity(cart.get(product))
                        .build())
            .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
            .collect(Collectors.toList());

    ShoppingCartApi.Cart apiCart = // <2>
        ShoppingCartApi.Cart.newBuilder().addAllItems(allItems).build();

    return effects().reply(apiCart);
  }
  // end::getCart[]

  // tag::removeCart[]
  @Override
  public Effect<Empty> removeCart(
      ReplicatedCounterMap<ShoppingCartDomain.Product> cart,
      ShoppingCartApi.RemoveShoppingCart removeShoppingCart) {

    return effects()
        .delete() // <1>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::removeCart[]
}
