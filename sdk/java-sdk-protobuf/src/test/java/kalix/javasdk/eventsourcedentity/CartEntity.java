/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.eventsourcedentity;

import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.grpc.Status.Code.INVALID_ARGUMENT;

/** User implementation of entity */
public class CartEntity extends AbstractCartEntity {

  public CartEntity(EventSourcedEntityContext context) {}

  @Override
  public ShoppingCartDomain.Cart emptyState() {
    return ShoppingCartDomain.Cart.getDefaultInstance();
  }

  @Override
  public Effect<Empty> addItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItem command) {
    if (command.getQuantity() <= 0) {
      return effects()
          .error(
              "Quantity for item " + command.getProductId() + " must be greater than zero.",
              INVALID_ARGUMENT);
    } else {
      return effects()
          .emitEvent(createItemAddedEvent(command))
          .thenReply(newState -> Empty.getDefaultInstance());
    }
  }

  private ShoppingCartDomain.ItemAdded createItemAddedEvent(ShoppingCartApi.AddLineItem command) {
    return createItemAddedEvent(command.getProductId(), command.getName(), command.getQuantity());
  }

  private ShoppingCartDomain.ItemAdded createItemAddedEvent(ShoppingCartApi.LineItem item) {
    return createItemAddedEvent(item.getProductId(), item.getName(), item.getQuantity());
  }

  private ShoppingCartDomain.ItemAdded createItemAddedEvent(
      String productId, String name, int quantity) {
    return ShoppingCartDomain.ItemAdded.newBuilder()
        .setItem(
            ShoppingCartDomain.LineItem.newBuilder()
                .setProductId(productId)
                .setName(name)
                .setQuantity(quantity)
                .build())
        .build();
  }

  @Override
  public Effect<Empty> addItems(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItems command) {
    if (command.getItemsList().stream().anyMatch(item -> item.getQuantity() <= 0)) {
      return effects().error("Quantity for items must be greater than zero.");
    } else {
      List<ShoppingCartDomain.ItemAdded> events =
          command.getItemsList().stream()
              .map(this::createItemAddedEvent)
              .collect(Collectors.toList());
      return effects().emitEvents(events).thenReply(newState -> Empty.getDefaultInstance());
    }
  }

  @Override
  public Effect<Empty> removeItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveLineItem command) {
    throw new RuntimeException("Boom: " + command.getProductId()); // always fail for testing
  }

  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.GetShoppingCart command) {

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
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemAdded event) {
    if (event.getItem().getName().equals("FAIL"))
      throw new RuntimeException("Boom: name is FAIL"); // fail for testing

    ShoppingCartDomain.LineItem item = event.getItem();
    ShoppingCartDomain.LineItem lineItem = updateItem(item, currentState);
    List<ShoppingCartDomain.LineItem> lineItems =
        removeItemByProductId(currentState, item.getProductId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    return ShoppingCartDomain.Cart.newBuilder().addAllItems(lineItems).build();
  }

  @Override
  public ShoppingCartDomain.Cart itemRemoved(
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemRemoved event) {
    throw new RuntimeException("Boom event: " + event.getProductId()); // always fail for testing
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
