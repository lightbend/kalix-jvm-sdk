/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import com.example.replicatedentity.shoppingcart.ShoppingCartApi;
import com.example.replicatedentity.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

public abstract class AbstractCartEntity
    extends ReplicatedRegisterMapEntity<String, ShoppingCartDomain.LineItem> {

  public abstract Effect<Empty> addItem(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> currentData,
      ShoppingCartApi.AddLineItem addLineItem);

  public abstract Effect<Empty> removeItem(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> currentData,
      ShoppingCartApi.RemoveLineItem removeLineItem);

  public abstract Effect<ShoppingCartApi.Cart> getCart(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> currentData,
      ShoppingCartApi.GetShoppingCart getShoppingCart);

  public abstract Effect<Empty> removeCart(
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> currentData,
      ShoppingCartApi.RemoveShoppingCart removeShoppingCart);
}
