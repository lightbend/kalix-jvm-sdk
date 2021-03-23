/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.view.Handler;
import com.akkaserverless.javasdk.view.HandlerContext;
import com.akkaserverless.javasdk.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.model.ShoppingCart;
import shopping.cart.view.ShoppingCartViewModel;

import java.util.Optional;

@View
public class ShoppingCartView {

  private static Logger LOG = LoggerFactory.getLogger(ShoppingCartView.class);

  @Handler
  public ShoppingCartViewModel.CartViewState processAdded(
      ShoppingCart.ItemAdded event,
      Optional<ShoppingCartViewModel.CartViewState> state,
      HandlerContext context) {
    if (state.isPresent()) {
      String userId = state.get().getUserId();
      int newNumberOfItems = state.get().getNumberOfItems() + event.getItem().getQuantity();
      LOG.info("Cart {} has {} items", userId, newNumberOfItems);
      return state.get().toBuilder().setNumberOfItems(newNumberOfItems).build();
    } else {
      String userId =
          context
              .sourceEntityId()
              .orElseGet(
                  () -> {
                    throw new IllegalArgumentException("Unknown sourceEntityId");
                  });
      int newNumberOfItems = event.getItem().getQuantity();
      LOG.info("New cart {} has {} items", userId, newNumberOfItems);
      return ShoppingCartViewModel.CartViewState.newBuilder()
          .setUserId(userId)
          .setNumberOfItems(newNumberOfItems)
          .build();
    }
  }

  @Handler
  public ShoppingCartViewModel.CartViewState processRemoved(
      ShoppingCart.ItemRemoved event, ShoppingCartViewModel.CartViewState state) {
    int newNumberOfItems = state.getNumberOfItems() - event.getQuantity();
    return state.toBuilder().setNumberOfItems(newNumberOfItems).build();
  }
}
