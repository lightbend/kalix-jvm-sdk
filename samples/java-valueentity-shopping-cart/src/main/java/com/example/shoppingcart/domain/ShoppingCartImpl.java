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

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A value entity.
 */
@ValueEntity(entityType = "shopping-cart")
public class ShoppingCartImpl extends ShoppingCartInterface {
  @SuppressWarnings("unused")
  private final String entityId;

  public ShoppingCartImpl(@EntityId String entityId) {
    this.entityId = entityId;
  }

  // tag::add-item[]
  @Override
  protected Empty addItem(ShoppingCartApi.AddLineItem command, CommandContext<ShoppingCartDomain.Cart> ctx) {
    if (command.getQuantity() <= 0) {
      throw ctx.fail("Cannot add negative quantity of to item " + command.getProductId());
    }

    ShoppingCartDomain.Cart cart =
        ctx.getState().orElse(ShoppingCartDomain.Cart.newBuilder().build());
    ShoppingCartDomain.LineItem lineItem = updateItem(command, cart);
    List<ShoppingCartDomain.LineItem> lineItems = removeItemByProductId(cart, command.getProductId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    ctx.updateState(ShoppingCartDomain.Cart.newBuilder().addAllItems(lineItems).build());
    return Empty.getDefaultInstance();
  }
  // end::add-item[]

  @Override
  protected Empty removeItem(ShoppingCartApi.RemoveLineItem command, CommandContext<ShoppingCartDomain.Cart> ctx) {
    ShoppingCartDomain.Cart cart =
        ctx.getState().orElse(ShoppingCartDomain.Cart.newBuilder().build());
    Optional<ShoppingCartDomain.LineItem> lineItem = findItemByProductId(cart, command.getProductId());

    if (!lineItem.isPresent()) {
      throw ctx.fail(
          "Cannot remove item " + command.getProductId() + " because it is not in the cart.");
    }

    List<ShoppingCartDomain.LineItem> items = removeItemByProductId(cart, command.getProductId());
    items.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    ctx.updateState(ShoppingCartDomain.Cart.newBuilder().addAllItems(items).build());
    return Empty.getDefaultInstance();
  }

  // tag::get-cart[]
  @Override
  protected ShoppingCartApi.Cart getCart(ShoppingCartApi.GetShoppingCart command, CommandContext<ShoppingCartDomain.Cart> ctx) {
    ShoppingCartDomain.Cart cart =
        ctx.getState().orElse(ShoppingCartDomain.Cart.newBuilder().build());
    List<ShoppingCartApi.LineItem> allItems =
        cart.getItemsList().stream()
            .map(this::convert)
            .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
            .collect(Collectors.toList());
    return ShoppingCartApi.Cart.newBuilder().addAllItems(allItems).build();
  }
  // end::get-cart[]

  @Override
  protected Empty removeCart(ShoppingCartApi.RemoveShoppingCart command, CommandContext<ShoppingCartDomain.Cart> ctx) {
    ctx.deleteState();
    return Empty.getDefaultInstance();
  }


  private ShoppingCartDomain.LineItem updateItem(
      ShoppingCartApi.AddLineItem item, ShoppingCartDomain.Cart cart) {
    return findItemByProductId(cart, item.getProductId())
        .map(li -> li.toBuilder().setQuantity(li.getQuantity() + item.getQuantity()).build())
        .orElse(newItem(item));
  }

  private ShoppingCartDomain.LineItem newItem(ShoppingCartApi.AddLineItem item) {
    return ShoppingCartDomain.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }

  private Optional<ShoppingCartDomain.LineItem> findItemByProductId(
      ShoppingCartDomain.Cart cart, String productId) {
    Predicate<ShoppingCartDomain.LineItem> lineItemExists =
        lineItem -> lineItem.getProductId().equals(productId);
    return cart.getItemsList().stream().filter(lineItemExists).findFirst();
  }

  private List<ShoppingCartDomain.LineItem> removeItemByProductId(
      ShoppingCartDomain.Cart cart, String productId) {
    return cart.getItemsList().stream()
        .filter(lineItem -> !lineItem.getProductId().equals(productId))
        .collect(Collectors.toList());
  }

  private ShoppingCartApi.LineItem convert(ShoppingCartDomain.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
}