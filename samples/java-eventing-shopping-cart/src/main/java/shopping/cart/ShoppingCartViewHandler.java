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

package shopping.cart;

import com.akkaserverless.javasdk.impl.view.ViewException;
import com.akkaserverless.javasdk.impl.view.ViewHandler;
import com.akkaserverless.javasdk.view.UpdateContext;
import com.akkaserverless.javasdk.view.View;
import scala.Option;
import shopping.cart.domain.ShoppingCartDomain;
import shopping.cart.view.ShoppingCartViewModel;

// FIXME to be generated
public class ShoppingCartViewHandler
    extends ViewHandler<ShoppingCartViewModel.CartViewState, ShoppingCartView> {

  public ShoppingCartViewHandler(ShoppingCartView view) {
    super(view);
  }

  @Override
  public View.UpdateEffect<ShoppingCartViewModel.CartViewState> handleUpdate(
      String commandName,
      ShoppingCartViewModel.CartViewState state,
      Object message,
      UpdateContext context) {
    switch (commandName) {
      case "ItemAdded":
        return view().processItemAdded(state, (ShoppingCartDomain.ItemAdded) message);

      case "ItemRemoved":
        return view().processItemRemoved(state, (ShoppingCartDomain.ItemRemoved) message);

      case "CheckedOut":
        return view().processCheckedOut(state, (ShoppingCartDomain.CheckedOut) message);

      default:
        throw new ViewException(
            context.viewId(),
            context.commandName(),
            "No command handler found for command ["
                + context.commandName()
                + "] on "
                + view().getClass().toString(),
            Option.empty());
    }
  }
}
