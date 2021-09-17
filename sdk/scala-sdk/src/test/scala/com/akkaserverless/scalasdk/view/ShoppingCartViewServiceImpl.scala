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

package com.akkaserverless.scalasdk.view

import com.akkaserverless.javasdk.shoppingcart.ShoppingCart
import com.akkaserverless.javasdk.shoppingcart.ShoppingCartViewModel

// FIXME this can be removed, unless it evolves into usage in tests

class ShoppingCartViewServiceImpl extends View[ShoppingCartViewModel.CartViewState] {
  override def emptyState: ShoppingCartViewModel.CartViewState =
    ShoppingCartViewModel.CartViewState.getDefaultInstance

  def processAdded(
      state: ShoppingCartViewModel.CartViewState,
      event: ShoppingCart.ItemAdded): View.UpdateEffect[ShoppingCartViewModel.CartViewState] = {
    effects().ignore() // TODO
  }

  // TODO update methods...
}
