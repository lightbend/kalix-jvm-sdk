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

import com.akkaserverless.javasdk.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.domain.ShoppingCartDomain;
import shopping.cart.view.ShoppingCartViewModel;

// user view impl
public class ShoppingCartView extends AbstractShoppingCartView {

  private static Logger LOG = LoggerFactory.getLogger(ShoppingCartView.class);

  public ShoppingCartView(ViewContext context) {}

  @Override
  public ShoppingCartViewModel.CartViewState emptyState() {
    return null;
  }

  @Override
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processItemAdded(
      ShoppingCartViewModel.CartViewState state, ShoppingCartDomain.ItemAdded itemAdded) {
    if (state != null) {
      String cartId = state.getCartId();
      int newNumberOfItems = state.getNumberOfItems() + itemAdded.getItem().getQuantity();
      LOG.info("Cart {} has {} items", cartId, newNumberOfItems);
      return updateEffects()
          .updateState(state.toBuilder().setNumberOfItems(newNumberOfItems).build());
    } else {
      String cartId =
          updateContext()
              .eventSubject()
              .orElseGet(
                  () -> {
                    throw new IllegalArgumentException("Unknown eventSubject");
                  });
      int newNumberOfItems = itemAdded.getItem().getQuantity();
      LOG.info("New cart {} has {} items", cartId, newNumberOfItems);
      return updateEffects()
          .updateState(
              ShoppingCartViewModel.CartViewState.newBuilder()
                  .setCartId(cartId)
                  .setNumberOfItems(newNumberOfItems)
                  .build());
    }
  }

  @Override
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processItemRemoved(
      ShoppingCartViewModel.CartViewState state, ShoppingCartDomain.ItemRemoved itemRemoved) {
    if (state == null)
      throw new IllegalArgumentException(
          "Cannot remove item from unknown cart " + updateContext().eventSubject());
    int newNumberOfItems = state.getNumberOfItems() - itemRemoved.getQuantity();
    return updateEffects()
        .updateState(state.toBuilder().setNumberOfItems(newNumberOfItems).build());
  }

  @Override
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processCheckedOut(
      ShoppingCartViewModel.CartViewState state, ShoppingCartDomain.CheckedOut checkedOut) {
    if (state == null)
      throw new IllegalArgumentException(
          "Cannot check out unknown cart " + updateContext().eventSubject());
    return updateEffects()
        .updateState(
            state.toBuilder().setCheckedOutTimestamp(checkedOut.getCheckedOutTimestamp()).build());
  }
}
