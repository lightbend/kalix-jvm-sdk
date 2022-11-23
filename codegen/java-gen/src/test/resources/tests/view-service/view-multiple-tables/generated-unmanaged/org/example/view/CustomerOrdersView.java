package org.example.view;

import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the View Service described in your customer_orders.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerOrdersView extends AbstractCustomerOrdersView {

  public CustomerOrdersView(ViewContext context) {}

  @Override
  public Object emptyState() {
    switch (updateContext().viewTable()) {
      case "customers":
        throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state for 'customers'");

      case "products":
        throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state for 'products'");

      default:
        return null;
    }
  }

  @Override
  public View.UpdateEffect<CustomerOrdersViewModel.CustomerState> updateCustomerCreated(
    CustomerOrdersViewModel.CustomerState state, CustomerOrdersViewModel.CustomerCreated customerCreated) {
    throw new UnsupportedOperationException("Update handler for 'UpdateCustomerCreated' not implemented yet");
  }

  @Override
  public View.UpdateEffect<CustomerOrdersViewModel.CustomerState> updateCustomerNameChanged(
    CustomerOrdersViewModel.CustomerState state, CustomerOrdersViewModel.CustomerNameChanged customerNameChanged) {
    throw new UnsupportedOperationException("Update handler for 'UpdateCustomerNameChanged' not implemented yet");
  }

  @Override
  public View.UpdateEffect<CustomerOrdersViewModel.ProductState> updateProduct(
    CustomerOrdersViewModel.ProductState state, CustomerOrdersViewModel.ProductCreated productCreated) {
    throw new UnsupportedOperationException("Update handler for 'UpdateProduct' not implemented yet");
  }

}

