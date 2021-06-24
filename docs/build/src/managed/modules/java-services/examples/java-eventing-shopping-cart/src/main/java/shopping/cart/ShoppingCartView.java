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

import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.UpdateHandlerContext;
import com.akkaserverless.javasdk.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.domain.ShoppingCartDomain;
import shopping.cart.view.ShoppingCartViewModel;

import java.util.Optional;

@View
public class ShoppingCartView {

  private static Logger LOG = LoggerFactory.getLogger(ShoppingCartView.class);

  @UpdateHandler
  public ShoppingCartViewModel.CartViewState processAdded(
      ShoppingCartDomain.ItemAdded event,
      Optional<ShoppingCartViewModel.CartViewState> state,
      UpdateHandlerContext context) {
    if (state.isPresent()) {
      String cartId = state.get().getCartId();
      int newNumberOfItems = state.get().getNumberOfItems() + event.getItem().getQuantity();
      LOG.info("Cart {} has {} items", cartId, newNumberOfItems);
      return state.get().toBuilder().setNumberOfItems(newNumberOfItems).build();
    } else {
      String cartId =
          context
              .eventSubject()
              .orElseGet(
                  () -> {
                    throw new IllegalArgumentException("Unknown eventSubject");
                  });
      int newNumberOfItems = event.getItem().getQuantity();
      LOG.info("New cart {} has {} items", cartId, newNumberOfItems);
      return ShoppingCartViewModel.CartViewState.newBuilder()
          .setCartId(cartId)
          .setNumberOfItems(newNumberOfItems)
          .build();
    }
  }

  @UpdateHandler
  public ShoppingCartViewModel.CartViewState processRemoved(
      ShoppingCartDomain.ItemRemoved event, ShoppingCartViewModel.CartViewState state) {
    int newNumberOfItems = state.getNumberOfItems() - event.getQuantity();
    return state.toBuilder().setNumberOfItems(newNumberOfItems).build();
  }

  @UpdateHandler
  public ShoppingCartViewModel.CartViewState processCheckedOut(
      ShoppingCartDomain.CheckedOut event, ShoppingCartViewModel.CartViewState state) {
    return state.toBuilder().setCheckedOutTimestamp(event.getCheckedOutTimestamp()).build();
  }
}
