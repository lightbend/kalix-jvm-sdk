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
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
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
    if (command.getQuantity() <= 0) { // <1>
      return effects().error("Cannot add negative quantity of to item" + command.getProductId());
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
}
