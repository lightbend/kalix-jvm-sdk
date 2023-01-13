package org.example.view;

import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the View Service described in your customer_orders.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class AnotherCustomerOrdersViewImpl extends AbstractAnotherCustomerOrdersView {

  public AnotherCustomerOrdersViewImpl(ViewContext context) {
    super(context);
  }

  @Override
  public CustomersViewTable createCustomersViewTable(ViewContext context) {
    return new CustomersViewTable(context);
  }

  public static class CustomersViewTable extends AbstractCustomersViewTable {

    public CustomersViewTable(ViewContext context) {}

    @Override
    public CustomerOrdersViewModel.Customer emptyState() {
      throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
    }

    @Override
    public View.UpdateEffect<CustomerOrdersViewModel.Customer> updateCustomerCreated(
        CustomerOrdersViewModel.Customer state,
        CustomerOrdersViewModel.CustomerCreated customerCreated) {
      throw new UnsupportedOperationException("Update handler for 'UpdateCustomerCreated' not implemented yet");
    }

    @Override
    public View.UpdateEffect<CustomerOrdersViewModel.Customer> updateCustomerNameChanged(
        CustomerOrdersViewModel.Customer state,
        CustomerOrdersViewModel.CustomerNameChanged customerNameChanged) {
      throw new UnsupportedOperationException("Update handler for 'UpdateCustomerNameChanged' not implemented yet");
    }

  }

  @Override
  public ProductsViewTable createProductsViewTable(ViewContext context) {
    return new ProductsViewTable(context);
  }

  public static class ProductsViewTable extends AbstractProductsViewTable {

    public ProductsViewTable(ViewContext context) {}

    @Override
    public CustomerOrdersViewModel.Product emptyState() {
      throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
    }

    @Override
    public View.UpdateEffect<CustomerOrdersViewModel.Product> updateProduct(
        CustomerOrdersViewModel.Product state,
        CustomerOrdersViewModel.ProductCreated productCreated) {
      throw new UnsupportedOperationException("Update handler for 'UpdateProduct' not implemented yet");
    }

  }

}

