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

import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound
import com.akkaserverless.javasdk.shoppingcart.ShoppingCart
import com.akkaserverless.javasdk.shoppingcart.ShoppingCartViewModel
import com.akkaserverless.scalasdk.impl.view.ViewHandler

// FIXME this can be removed, unless it evolves into usage in tests

class ShoppingCartViewServiceHandler(view: ShoppingCartViewServiceImpl)
    extends ViewHandler[ShoppingCartViewModel.CartViewState, ShoppingCartViewServiceImpl](view) {

  override def handleUpdate(
      commandName: String,
      state: ShoppingCartViewModel.CartViewState,
      event: Any): View.UpdateEffect[ShoppingCartViewModel.CartViewState] = {
    commandName match {
      case "ProcessAdded" => view.processAdded(state, event.asInstanceOf[ShoppingCart.ItemAdded])
      // TODO more ...
      case _ => throw new UpdateHandlerNotFound(commandName)
    }
  }
}
