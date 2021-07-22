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

package com.akkaserverless.javasdk.eventsourcedentity;

import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

/** User implementation of entity */
public class CartEntity extends AbstractCartEntity {
  @Override
  public ShoppingCartDomain.Cart emptyState() {
    return ShoppingCartDomain.Cart.getDefaultInstance();
  }

  @Override
  public Effect<Empty> addItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.AddLineItem command) {
    return null;
  }

  @Override
  public Effect<Empty> removeItem(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.RemoveLineItem command) {
    return null;
  }

  @Override
  public Effect<ShoppingCartApi.Cart> getCart(
      ShoppingCartDomain.Cart currentState, ShoppingCartApi.GetShoppingCart command) {
    return null;
  }

  @Override
  public ShoppingCartDomain.Cart itemAdded(
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemAdded event) {
    return null;
  }

  @Override
  public ShoppingCartDomain.Cart itemRemoved(
      ShoppingCartDomain.Cart currentState, ShoppingCartDomain.ItemRemoved event) {
    return null;
  }
}
