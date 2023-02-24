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

import kalix.javasdk.eventsourcedentity.CommandContext;
import kalix.javasdk.eventsourcedentity.EventContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// tag::class[]
public class ShoppingCart extends AbstractShoppingCart { // <1>
  @SuppressWarnings("unused")
  private final String entityId;

  public ShoppingCart(EventSourcedEntityContext context) { this.entityId = context.entityId(); }

  @Override
  public ShoppingCartDomain.Cart emptyState() { // <2>
    return ShoppingCartDomain.Cart.getDefaultInstance();
  }
  // end::class[]

  // tag::addItem[]
  @Override
  public Effect<Empty> addItem(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartApi.AddLineItem command) {
    if (currentState.getCheckedOut())
      return effects().error("Cart is already checked out.");
    if (command.getQuantity() <= 0) { // <1>
      return effects().error("Quantity for item " + command.getProductId() + " must be greater than zero.");
    }

    ShoppingCartDomain.ItemAdded event = // <2>
        ShoppingCartDomain.ItemAdded.newBuilder()
            .setItem(
                ShoppingCartDomain.LineItem.newBuilder()
                    .setProductId(command.getProductId())
                    .setName(command.getName())
                    .setQuantity(command.getQuantity())
                    .build())
            .build();

    return effects()
            .emitEvent(event) // <3>
            .thenReply(newState -> Empty.getDefaultInstance()); // <4>
  }
  // end::addItem[]

  @Override
  public Effect<Empty> removeItem(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartApi.RemoveLineItem command) {
    if (currentState.getCheckedOut())
      return effects().error("Cart is already checked out.");
    if (findItemByProductId(currentState, command.getProductId()).isEmpty()) {
      return effects()
          .error(
              "Cannot remove item " + command.getProductId() + " because it is not in the cart.");
    }

    ShoppingCartDomain.ItemRemoved event =
        ShoppingCartDomain.ItemRemoved.newBuilder().setProductId(command.getProductId()).build();

    return effects()
            .emitEvent(event)
            .thenReply(newState -> Empty.getDefaultInstance());
  }

  // tag::getCart[]
  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
      ShoppingCartDomain.Cart currentState, // <1>
      ShoppingCartApi.GetShoppingCart command) {
    List<ShoppingCartApi.LineItem> apiItems =
        currentState.getItemsList().stream()
            .map(this::convert)
            .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
            .collect(Collectors.toList());
    ShoppingCartApi.Cart apiCart =
            ShoppingCartApi.Cart.newBuilder().addAllItems(apiItems)
                .setCheckedOut(currentState.getCheckedOut())
                .build(); // <2>
    return effects().reply(apiCart);
  }

  private ShoppingCartApi.LineItem convert(ShoppingCartDomain.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
            .setProductId(item.getProductId())
            .setName(item.getName())
            .setQuantity(item.getQuantity())
            .build();
  }
  // end::getCart[]

  // tag::checkout[]
  @Override
  public Effect<Empty> checkout(ShoppingCartDomain.Cart currentState, ShoppingCartApi.CheckoutShoppingCart checkoutShoppingCart) {
    if (currentState.getCheckedOut())
      return effects().error("Cart is already checked out.");
    return effects()
        .emitEvent(ShoppingCartDomain.CheckedOut.getDefaultInstance()) // <1>
        .deleteEntity() // <2>
        .thenReply(newState -> Empty.getDefaultInstance());
  }
  // end::checkout[]

  // tag::itemAdded[]
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

  // end::itemAdded[]

  @Override
  public ShoppingCartDomain.Cart itemRemoved(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartDomain.ItemRemoved itemRemoved) {
    List<ShoppingCartDomain.LineItem> items =
        removeItemByProductId(currentState, itemRemoved.getProductId());
    items.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    return ShoppingCartDomain.Cart.newBuilder().addAllItems(items).build();
  }

  // tag::itemAdded[]
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
  // end::itemAdded[]

  @Override
  public ShoppingCartDomain.Cart checkedOut(
      ShoppingCartDomain.Cart currentState,
      ShoppingCartDomain.CheckedOut checkedOut) {
    return ShoppingCartDomain.Cart.newBuilder(currentState).setCheckedOut(true).build();
  }
}
