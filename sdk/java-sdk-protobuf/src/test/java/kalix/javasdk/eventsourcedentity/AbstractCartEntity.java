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

/**
 * Generated entity baseclass, extended by user entity impl, helps getting the impl in sync with
 * protobuf def
 */
public abstract class AbstractCartEntity extends EventSourcedEntity<ShoppingCartDomain.Cart, Object> {

  public abstract Effect<Empty> addItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItem command);

  public abstract Effect<Empty> addItems(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItems command);

  public abstract Effect<Empty> removeItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveLineItem command);

  public abstract Effect<ShoppingCartApi.Cart> getCart(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.GetShoppingCart command);

  public abstract ShoppingCartDomain.Cart itemAdded(
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemAdded event);

  public abstract ShoppingCartDomain.Cart itemRemoved(
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemRemoved event);
}
