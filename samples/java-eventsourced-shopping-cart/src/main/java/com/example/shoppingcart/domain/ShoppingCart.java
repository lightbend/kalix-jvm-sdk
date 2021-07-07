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

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@EventSourcedEntity(entityType = "eventsourced-shopping-cart")
public class ShoppingCart extends AbstractShoppingCart {
  @SuppressWarnings("unused")
  private final String entityId;

  private final Map<String, ShoppingCartApi.LineItem> cart = new LinkedHashMap<>();

  public ShoppingCart(@EntityId String entityId) {
    this.entityId = entityId;
  }

  @Override
  public ShoppingCartDomain.Cart emptyState() {
    return ShoppingCartDomain.Cart.getDefaultInstance();
  }

  @Override
  public Effect<Empty> addItem(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartApi.AddLineItem command) {
    if (command.getQuantity() <= 0) {
      return effects().error("Cannot add negative quantity of to item" + command.getProductId());
    }

    ShoppingCartDomain.ItemAdded event =
        ShoppingCartDomain.ItemAdded.newBuilder()
            .setItem(
                ShoppingCartDomain.LineItem.newBuilder()
                    .setProductId(command.getProductId())
                    .setName(command.getName())
                    .setQuantity(command.getQuantity())
                    .build())
            .build();

    return effects().emitEvent(event).thenReply(newState -> Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> removeItem(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartApi.RemoveLineItem command) {
    if (findItemByProductId(currentState, command.getProductId()).isEmpty()) {
      return effects()
          .error(
              "Cannot remove item " + command.getProductId() + " because it is not in the cart.");
    }

    ShoppingCartDomain.ItemRemoved event =
        ShoppingCartDomain.ItemRemoved.newBuilder().setProductId(command.getProductId()).build();

    return effects().emitEvent(event).thenReply(newState -> Empty.getDefaultInstance());
  }

  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartApi.GetShoppingCart command) {
    List<ShoppingCartApi.LineItem> apiItems =
        currentState.getItemsList().stream()
            .map(this::convert)
            .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
            .collect(Collectors.toList());
    ShoppingCartApi.Cart apiCart = ShoppingCartApi.Cart.newBuilder().addAllItems(apiItems).build();
    return effects().reply(apiCart);
  }

  @Override
  public ShoppingCartDomain.Cart itemAdded(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartDomain.ItemAdded itemAdded) {
    ShoppingCartDomain.LineItem item = itemAdded.getItem();
    ShoppingCartDomain.LineItem lineItem = updateItem(item, currentState);
    List<ShoppingCartDomain.LineItem> lineItems =
        removeItemByProductId(currentState, item.getProductId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    return ShoppingCartDomain.Cart.newBuilder().addAllItems(lineItems).build();
  }

  @Override
  public ShoppingCartDomain.Cart itemRemoved(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartDomain.ItemRemoved itemRemoved) {
    List<ShoppingCartDomain.LineItem> items =
        removeItemByProductId(currentState, itemRemoved.getProductId());
    items.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    return ShoppingCartDomain.Cart.newBuilder().addAllItems(items).build();
  }

  private ShoppingCartApi.LineItem convert(ShoppingCartDomain.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }

  private ShoppingCartDomain.LineItem updateItem(
      ShoppingCartDomain.LineItem item, ShoppingCartDomain.Cart cart) {
    return findItemByProductId(cart, item.getProductId())
        .map(li -> li.toBuilder().setQuantity(li.getQuantity() + item.getQuantity()).build())
        .orElse(item);
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
}
