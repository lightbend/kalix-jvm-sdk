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

// tag::class[]
package shopping.cart;

// end::class[]
import com.akkaserverless.javasdk.EntityId;
// tag::addItem[]
import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.google.protobuf.Empty;
// end::addItem[]
import shopping.cart.api.ShoppingCartApi;
import shopping.cart.domain.ShoppingCartDomain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** An event sourced entity. */
// tag::class[]

@EventSourcedEntity(entityType = "eventsourced-shopping-cart")
public class ShoppingCartEntity {
  // tag::state[]
  // tag::itemAdded[]
  private final String entityId;
  private final Map<String, ShoppingCartApi.LineItem> cart = new LinkedHashMap<>();
  private long checkedOutTimestamp = 0L;
  // end::itemAdded[]
  // end::state[]

  // tag::constructor[]
  public ShoppingCartEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }
  // end::constructor[]
  // end::class[]

  // tag::snapshot[]
  @Snapshot
  public ShoppingCartDomain.CartState snapshot() {
    return ShoppingCartDomain.CartState.newBuilder()
        .addAllItems(cart.values().stream().map(this::convert).collect(Collectors.toList()))
        .setCheckedOutTimestamp(checkedOutTimestamp)
        .build();
  }

  private ShoppingCartDomain.LineItem convert(ShoppingCartApi.LineItem item) {
    return ShoppingCartDomain.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
  // end::snapshot[]

  // tag::handleSnapshot[]
  @SnapshotHandler
  public void handleSnapshot(ShoppingCartDomain.CartState cart) {
    this.cart.clear();
    for (ShoppingCartDomain.LineItem item : cart.getItemsList()) {
      this.cart.put(item.getProductId(), convert(item));
    }
    this.checkedOutTimestamp = cart.getCheckedOutTimestamp();
  }
  // end::handleSnapshot[]

  // tag::itemAdded[]

  @EventHandler
  public void itemAdded(ShoppingCartDomain.ItemAdded itemAdded) {
    ShoppingCartApi.LineItem item = cart.get(itemAdded.getItem().getProductId());
    if (item == null) {
      item = convert(itemAdded.getItem());
    } else {
      item =
          item.toBuilder()
              .setQuantity(item.getQuantity() + itemAdded.getItem().getQuantity())
              .build();
    }
    cart.put(item.getProductId(), item);
  }

  private ShoppingCartApi.LineItem convert(ShoppingCartDomain.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
  // end::itemAdded[]

  @EventHandler
  public void itemRemoved(ShoppingCartDomain.ItemRemoved itemRemoved) {
    ShoppingCartApi.LineItem lineItem = cart.get(itemRemoved.getProductId());
    int newQty = lineItem.getQuantity() - itemRemoved.getQuantity();

    if (newQty > 0) {
      ShoppingCartApi.LineItem newItemLine = lineItem.toBuilder().setQuantity(newQty).build();
      cart.put(itemRemoved.getProductId(), newItemLine);
    } else {
      cart.remove(itemRemoved.getProductId());
    }
  }

  @EventHandler
  public void checkedOut(ShoppingCartDomain.CheckedOut checkedOut) {
    checkedOutTimestamp = checkedOut.getCheckedOutTimestamp();
  }

  // tag::getCart[]
  @CommandHandler
  public ShoppingCartApi.Cart getCart() {
    return createApiCart();
  }
  // end::getCart[]

  // tag::addItem[]

  @CommandHandler
  public Empty addItem(ShoppingCartApi.AddLineItem item, CommandContext context) {
    if (checkedOutTimestamp > 0) {
      throw context.fail("Cannot add item to checked out cart.");
    }
    if (item.getQuantity() <= 0) {
      throw context.fail("Cannot add negative quantity of to item" + item.getProductId());
    }
    ShoppingCartDomain.ItemAdded itemAddedEvent =
        ShoppingCartDomain.ItemAdded.newBuilder()
            .setItem(
                ShoppingCartDomain.LineItem.newBuilder()
                    .setProductId(item.getProductId())
                    .setName(item.getName())
                    .setQuantity(item.getQuantity())
                    .build())
            .build();
    context.emit(itemAddedEvent);
    return Empty.getDefaultInstance();
  }
  // end::addItem[]

  @CommandHandler
  public Empty removeItem(ShoppingCartApi.RemoveLineItem item, CommandContext context) {
    if (checkedOutTimestamp > 0) {
      throw context.fail("Cannot remove item from checked out cart.");
    }
    if (!cart.containsKey(item.getProductId())) {
      throw context.fail(
          "Cannot remove item " + item.getProductId() + " because it is not in the cart.");
    } else {
      ShoppingCartApi.LineItem lineItem = cart.get(item.getProductId());
      ShoppingCartDomain.ItemRemoved event = null;
      if ((lineItem.getQuantity() - item.getQuantity()) > 0) {
        event =
            ShoppingCartDomain.ItemRemoved.newBuilder()
                .setProductId(item.getProductId())
                .setQuantity(item.getQuantity()) // only remove requested quantity
                .build();
      } else {
        event =
            ShoppingCartDomain.ItemRemoved.newBuilder()
                .setProductId(item.getProductId())
                .setQuantity(lineItem.getQuantity()) // remove all
                .build();
      }
      context.emit(event);
    }
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public ShoppingCartApi.Cart checkoutCart(
      ShoppingCartApi.Checkout request, CommandContext context) {
    if (checkedOutTimestamp > 0) {
      throw context.fail("Cannot checkout an already checked out cart.");
    }
    context.emit(
        ShoppingCartDomain.CheckedOut.newBuilder()
            .setCheckedOutTimestamp(System.currentTimeMillis())
            .build());
    return createApiCart();
  }

  private ShoppingCartApi.Cart createApiCart() {
    return ShoppingCartApi.Cart.newBuilder()
        .addAllItems(cart.values())
        .setCheckedOutTimestamp(checkedOutTimestamp)
        .build();
  }
}
