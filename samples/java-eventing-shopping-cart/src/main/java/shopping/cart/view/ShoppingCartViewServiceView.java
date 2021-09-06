/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package shopping.cart.view;

import com.akkaserverless.javasdk.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.domain.ShoppingCartDomain;

public class ShoppingCartViewServiceView extends AbstractShoppingCartViewServiceView {

  private static Logger LOG = LoggerFactory.getLogger(ShoppingCartViewServiceView.class);

  public ShoppingCartViewServiceView(ViewContext context) {}

  @Override
  public ShoppingCartViewModel.CartViewState emptyState() {
    return null;
  }

  @Override
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processAdded(
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
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processRemoved(
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