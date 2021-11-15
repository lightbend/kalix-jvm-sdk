package com.example.shoppingcart.view;

import com.akkaserverless.javasdk.view.ViewContext;
import com.example.shoppingcart.domain.ShoppingCartDomain;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class ShoppingCartViewServiceImpl extends AbstractShoppingCartViewServiceView {

  public ShoppingCartViewServiceImpl(ViewContext context) {}

  @Override
  public ShoppingCartViewModel.CartViewState emptyState() {
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
  }

  @Override
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processAdded(
    ShoppingCartViewModel.CartViewState state, ShoppingCartDomain.ItemAdded itemAdded) {
    throw new UnsupportedOperationException("Update handler for 'ProcessAdded' not implemented yet");
  }
  @Override
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processRemoved(
    ShoppingCartViewModel.CartViewState state, ShoppingCartDomain.ItemRemoved itemRemoved) {
    throw new UnsupportedOperationException("Update handler for 'ProcessRemoved' not implemented yet");
  }
  @Override
  public UpdateEffect<ShoppingCartViewModel.CartViewState> processCheckedOut(
    ShoppingCartViewModel.CartViewState state, ShoppingCartDomain.CheckedOut checkedOut) {
    throw new UnsupportedOperationException("Update handler for 'ProcessCheckedOut' not implemented yet");
  }
}
