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

import kalix.javasdk.valueentity.ValueEntityContext;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A value entity.
 */
public class ShoppingCart extends AbstractShoppingCart {
  @SuppressWarnings("unused")
  private final String entityId;

  public ShoppingCart(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public ShoppingCartDomain.Cart emptyState() {
    return ShoppingCartDomain.Cart.newBuilder().build();
  }

  // tag::create[]
  @Override
  public Effect<Empty> create(ShoppingCartDomain.Cart currentState, ShoppingCartApi.CreateCart createCart) {
    if (currentState.getCreationTimestamp() > 0L) {
      return effects().error("Cart was already created");
    } else {
      return effects().updateState(currentState.toBuilder().setCreationTimestamp(Instant.now().toEpochMilli()).build())
          .thenReply(Empty.getDefaultInstance());
    }
  }
  // end::create[]

  // tag::add-item[]
  @Override
  public Effect<Empty> addItem(
          ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItem addLineItem) {
    if (addLineItem.getQuantity() <= 0) {
      return effects()
              .error("Quantity for item " + addLineItem.getProductId() + " must be greater than zero.");
    }

    ShoppingCartDomain.LineItem lineItem = updateItem(addLineItem, currentState);
    List<ShoppingCartDomain.LineItem> lineItems =
            removeItemByProductId(currentState, addLineItem.getProductId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    return effects()
            .updateState(currentState.toBuilder().clearItems().addAllItems(lineItems).build())
            .thenReply(Empty.getDefaultInstance());
  }
  // end::add-item[]
  @Override
  public Effect<Empty> removeItem(
          ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveLineItem removeLineItem) {
    Optional<ShoppingCartDomain.LineItem> lineItem =
            findItemByProductId(currentState, removeLineItem.getProductId());

    if (lineItem.isEmpty()) {
      return effects()
              .error(
                      "Cannot remove item "
                              + removeLineItem.getProductId()
                              + " because it is not in the cart.");
    }

    List<ShoppingCartDomain.LineItem> items =
            removeItemByProductId(currentState, removeLineItem.getProductId());
    items.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    return effects()
            .updateState(currentState.toBuilder().clearItems().addAllItems(items).build())
            .thenReply(Empty.getDefaultInstance());
  }

  // tag::get-cart[]
  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
          ShoppingCartDomain.Cart currentState, ShoppingCartApi.GetShoppingCart getShoppingCart) {
    List<ShoppingCartApi.LineItem> allItems =
            currentState.getItemsList().stream()
                    .map(this::convert)
                    .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
                    .collect(Collectors.toList());

    ShoppingCartApi.Cart apiCart = ShoppingCartApi.Cart.newBuilder()
        .addAllItems(allItems)
        .setCreationTimestamp(currentState.getCreationTimestamp())
        .build();

    return effects().reply(apiCart);
  }
  // end::get-cart[]

  @Override
  public Effect<Empty> removeCart(
          ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveShoppingCart removeShoppingCart) {
    var userRole = commandContext().metadata().get("Role").get();
    if (userRole.equals("Admin")) {
      return effects().deleteEntity().thenReply(Empty.getDefaultInstance());
    } else {
      return effects().error("Only admin can remove the cart");
    }
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
