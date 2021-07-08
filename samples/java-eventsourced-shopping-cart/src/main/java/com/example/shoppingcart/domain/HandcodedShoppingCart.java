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

import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;

import java.util.Optional;
import java.util.stream.Collectors;

public class HandcodedShoppingCart extends EventSourcedEntityBase<ShoppingCartDomain.Cart> {

  private final String id;
  
  public HandcodedShoppingCart(String id) {
    this.id = id;
  }

  @Override
  protected ShoppingCartDomain.Cart emptyState() {
    return ShoppingCartDomain.Cart.getDefaultInstance();
  }

  @CommandHandler
  public Effect<Empty> addItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItem command) {
    if (command.getQuantity() < 0) {
      return effects().error("Quantity cannot be negative for product " + command.getProductId());
    } else {
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
  }

  @CommandHandler
  public Effect<Empty> removeItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveLineItem command) {
    if (currentState.getItemsList().stream()
        .filter(item -> item.getProductId().equals(command.getProductId()))
        .findFirst()
        .isEmpty()) {
      return effects().error("Cannot remove product " + command.getProductId() + ", not in cart");
    }
    {
      ShoppingCartDomain.ItemRemoved event =
          ShoppingCartDomain.ItemRemoved.newBuilder().setProductId(command.getProductId()).build();
      return effects().emitEvent(event).thenReply(newSate -> Empty.getDefaultInstance());
    }
  }

  @CommandHandler
  public Effect<ShoppingCartApi.Cart> getCart(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.GetShoppingCart command) {

    ShoppingCartApi.Cart apiCart =
        ShoppingCartApi.Cart.newBuilder()
            .addAllItems(
                currentState.getItemsList().stream()
                    .map(
                        domainItem ->
                            ShoppingCartApi.LineItem.newBuilder()
                                .setProductId(domainItem.getProductId())
                                .setName(domainItem.getName())
                                .setQuantity(domainItem.getQuantity())
                                .build())
                    .collect(Collectors.toList()))
            .build();

    return effects().reply(apiCart);
  }

  @EventHandler
  protected ShoppingCartDomain.Cart itemAdded(
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemAdded event) {
    Optional<ShoppingCartDomain.LineItem> existingItem =
        currentState.getItemsList().stream()
            .filter(item -> item.getProductId().equals(event.getItem().getProductId()))
            .findFirst();
    if (existingItem.isPresent()) {
      // already exists in cart, replace (or perhaps add quantity would make sense)
      ShoppingCartDomain.LineItem updatedItem =
          existingItem.get().toBuilder().setQuantity(event.getItem().getQuantity()).build();
      return ShoppingCartDomain.Cart.newBuilder()
          .addAllItems(
              currentState.getItemsList().stream()
                  .filter(item -> !item.getProductId().equals(updatedItem.getProductId()))
                  .collect(Collectors.toList()))
          .addItems(updatedItem)
          .build();
    } else {
      ShoppingCartDomain.LineItem newItem =
          ShoppingCartDomain.LineItem.newBuilder()
              .setProductId(event.getItem().getProductId())
              .setName(event.getItem().getName())
              .setQuantity(event.getItem().getQuantity())
              .build();
      return currentState.toBuilder().addItems(newItem).build();
    }
  }

  @EventHandler
  protected ShoppingCartDomain.Cart itemRemoved(
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemRemoved event) {
    return ShoppingCartDomain.Cart.newBuilder()
        .addAllItems(
            currentState.getItemsList().stream()
                .filter(item -> !item.getProductId().equals(event.getProductId()))
                .collect(Collectors.toList()))
        .build();
  }
}
