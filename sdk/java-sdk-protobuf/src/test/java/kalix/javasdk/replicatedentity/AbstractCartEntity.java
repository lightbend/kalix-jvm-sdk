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
