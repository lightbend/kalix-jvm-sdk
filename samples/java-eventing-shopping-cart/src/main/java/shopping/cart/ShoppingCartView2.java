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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.domain.ShoppingCartDomain;
import shopping.cart.view.ShoppingCartViewModel;

import java.util.Optional;

// user view impl
public class ShoppingCartView2 extends AbstractShoppingCartView {

  private static Logger LOG = LoggerFactory.getLogger(ShoppingCartView2.class);

  public UpdateEffect<ShoppingCartViewModel.CartViewState> processItemAdded(
      ShoppingCartDomain.ItemAdded event,
      Optional<ShoppingCartViewModel.CartViewState> state) {
    if (state.isPresent()) {
      String cartId = state.get().getCartId();
      int newNumberOfItems = state.get().getNumberOfItems() + event.getItem().getQuantity();
      LOG.info("Cart {} has {} items", cartId, newNumberOfItems);
      return updateEffects().updateState(state.get().toBuilder().setNumberOfItems(newNumberOfItems).build());
    } else {
      String cartId =
          updateHandlerContext()
              .eventSubject()
              .orElseGet(
                  () -> {
                    throw new IllegalArgumentException("Unknown eventSubject");
                  });
      int newNumberOfItems = event.getItem().getQuantity();
      LOG.info("New cart {} has {} items", cartId, newNumberOfItems);
      return updateEffects().updateState(ShoppingCartViewModel.CartViewState.newBuilder()
          .setCartId(cartId)
          .setNumberOfItems(newNumberOfItems)
          .build());
    }
  }

  public UpdateEffect<ShoppingCartViewModel.CartViewState> processItemRemoved(
      ShoppingCartDomain.ItemRemoved event, ShoppingCartViewModel.CartViewState state) {
    int newNumberOfItems = state.getNumberOfItems() - event.getQuantity();
    return updateEffects().updateState(state.toBuilder().setNumberOfItems(newNumberOfItems).build());
  }

  public UpdateEffect<ShoppingCartViewModel.CartViewState> processCheckedOut(
      ShoppingCartDomain.CheckedOut event, ShoppingCartViewModel.CartViewState state) {
    return updateEffects().updateState(state.toBuilder().setCheckedOutTimestamp(event.getCheckedOutTimestamp()).build());
  }
}
