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

package kalix.javasdk.replicatedentity;

import com.example.replicatedentity.shoppingcart.ShoppingCartApi;
import com.example.replicatedentity.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.grpc.Status.Code.INVALID_ARGUMENT;

public class CartEntity extends AbstractCartEntity {
  @SuppressWarnings("unused")
  private final String entityId;

  public CartEntity(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Effect<Empty> addItem(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> cart,
      ShoppingCartApi.AddLineItem addLineItem) {
    if (addLineItem.getQuantity() <= 0) {
      return effects()
          .error(
              "Quantity for item " + addLineItem.getProductId() + " must be greater than zero.",
              INVALID_ARGUMENT);
    }

    return effects()
        .update(cart.setValue(addLineItem.getProductId(), updateItem(addLineItem, cart)))
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> removeItem(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> currentData,
      ShoppingCartApi.RemoveLineItem removeLineItem) {
    throw new RuntimeException("Boom: " + removeLineItem.getProductId()); // always fail for testing
  }

  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> currentData,
      ShoppingCartApi.GetShoppingCart getShoppingCart) {
    List<ShoppingCartApi.LineItem> allItems =
        currentData.keySet().stream()
            .map(currentData::getValue)
            .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
            .map(this::convert)
            .sorted(Comparator.comparing(ShoppingCartApi.LineItem::getProductId))
            .collect(Collectors.toList());
    return effects().reply(ShoppingCartApi.Cart.newBuilder().addAllItems(allItems).build());
  }

  @Override
  public Effect<Empty> removeCart(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> currentData,
      ShoppingCartApi.RemoveShoppingCart removeShoppingCart) {
    return effects().delete().thenReply(Empty.getDefaultInstance());
  }

  private ShoppingCartDomain.LineItem updateItem(
      ShoppingCartApi.AddLineItem item,
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> cart) {
    return cart.getValue(item.getProductId())
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

  private ShoppingCartApi.LineItem convert(ShoppingCartDomain.LineItem item) {
    return ShoppingCartApi.LineItem.newBuilder()
        .setProductId(item.getProductId())
        .setName(item.getName())
        .setQuantity(item.getQuantity())
        .build();
  }
}
