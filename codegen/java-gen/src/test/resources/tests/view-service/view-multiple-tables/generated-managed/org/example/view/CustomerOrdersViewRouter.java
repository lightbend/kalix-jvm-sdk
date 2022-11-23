package org.example.view;

import kalix.javasdk.impl.view.UpdateHandlerNotFound;
import kalix.javasdk.impl.view.ViewRouter;
import kalix.javasdk.view.View;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public class CustomerOrdersViewRouter extends ViewRouter<CustomerOrdersView> {

  public CustomerOrdersViewRouter(CustomerOrdersView view) {
    super(view);
  }

  @Override
  public <S> View.UpdateEffect<S> handleUpdate(
      String eventName,
      S state,
      Object event) {

    switch (eventName) {
      case "UpdateCustomerCreated":
        return (View.UpdateEffect<S>)
            view().updateCustomerCreated(
                (CustomerOrdersViewModel.CustomerState) state,
                (CustomerOrdersViewModel.CustomerCreated) event);

      case "UpdateCustomerNameChanged":
        return (View.UpdateEffect<S>)
            view().updateCustomerNameChanged(
                (CustomerOrdersViewModel.CustomerState) state,
                (CustomerOrdersViewModel.CustomerNameChanged) event);

      case "UpdateProduct":
        return (View.UpdateEffect<S>)
            view().updateProduct(
                (CustomerOrdersViewModel.ProductState) state,
                (CustomerOrdersViewModel.ProductCreated) event);

      default:
        throw new UpdateHandlerNotFound(eventName);
    }
  }

  @Override
  public String viewTable(String eventName, Object event) {
    switch (eventName) {
      case "UpdateCustomerCreated":
        return "customers";

      case "UpdateCustomerNameChanged":
        return "customers";

      case "UpdateProduct":
        return "products";

      default:
        return "";
    }
  }

}

