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

package kalix.javasdk.valueentity;

import com.example.valueentity.shoppingcart.ShoppingCartApi;
import com.example.valueentity.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.grpc.Status.Code.INVALID_ARGUMENT;

/** A value entity. */
public class CartEntity extends AbstractCartEntity {
  @SuppressWarnings("unused")
  private final String entityId;

  public CartEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public ShoppingCartDomain.Cart emptyState() {
    return ShoppingCartDomain.Cart.newBuilder().build();
  }

  @Override
  public Effect<Empty> addItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItem addLineItem) {
    if (addLineItem.getQuantity() <= 0) {
      return effects()
          .error(
              "Quantity for item " + addLineItem.getProductId() + " must be greater than zero.",
              INVALID_ARGUMENT);
    }

    ShoppingCartDomain.LineItem lineItem = updateItem(addLineItem, currentState);
    List<ShoppingCartDomain.LineItem> lineItems =
        removeItemByProductId(currentState, addLineItem.getProductId());
    lineItems.add(lineItem);
    lineItems.sort(Comparator.comparing(ShoppingCartDomain.LineItem::getProductId));
    return effects()
        .updateState(ShoppingCartDomain.Cart.newBuilder().addAllItems(lineItems).build())
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> removeItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveLineItem removeLineItem) {
    throw new RuntimeException("Boom: " + removeLineItem.getProductId()); // always fail for testing
  }

  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.GetShoppingCart getShoppingCart) {
    List<ShoppingCartApi.LineItem> allItems =
        currentState.getItemsList().stream()
            .map(this::convert)
            .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
            .collect(Collectors.toList());
    return effects().reply(ShoppingCartApi.Cart.newBuilder().addAllItems(allItems).build());
  }

  @Override
  public Effect<Empty> removeCart(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveShoppingCart removeShoppingCart) {
    return effects().deleteEntity().thenReply(Empty.getDefaultInstance());
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
