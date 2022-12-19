package org.example.view;

import kalix.javasdk.impl.view.UpdateHandlerNotFound;
import kalix.javasdk.impl.view.ViewMultiTableRouter;
import kalix.javasdk.impl.view.ViewRouter;
import kalix.javasdk.view.View;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public class CustomerOrdersViewRouter extends ViewMultiTableRouter {

  private CustomersViewTableRouter customersViewTableRouter;
  private ProductsViewTableRouter productsViewTableRouter;

  public CustomerOrdersViewRouter(CustomerOrdersView view) {
    customersViewTableRouter = new CustomersViewTableRouter(view.customersViewTable());
    productsViewTableRouter = new ProductsViewTableRouter(view.productsViewTable());
  }

  public static class CustomersViewTableRouter extends ViewRouter<CustomerOrdersViewModel.Customer, AbstractCustomerOrdersView.AbstractCustomersViewTable> {

    public CustomersViewTableRouter(AbstractCustomerOrdersView.AbstractCustomersViewTable view) {
      super(view);
    }

    @Override
    public View.UpdateEffect<CustomerOrdersViewModel.Customer> handleUpdate(
        String eventName,
        CustomerOrdersViewModel.Customer state,
        Object event) {

      switch (eventName) {
        case "UpdateCustomerCreated":
          return view().updateCustomerCreated(
              state,
              (CustomerOrdersViewModel.CustomerCreated) event);

        case "UpdateCustomerNameChanged":
          return view().updateCustomerNameChanged(
              state,
              (CustomerOrdersViewModel.CustomerNameChanged) event);

        default:
          throw new UpdateHandlerNotFound(eventName);
      }
    }

  }

  public static class ProductsViewTableRouter extends ViewRouter<CustomerOrdersViewModel.Product, AbstractCustomerOrdersView.AbstractProductsViewTable> {

    public ProductsViewTableRouter(AbstractCustomerOrdersView.AbstractProductsViewTable view) {
      super(view);
    }

    @Override
    public View.UpdateEffect<CustomerOrdersViewModel.Product> handleUpdate(
        String eventName,
        CustomerOrdersViewModel.Product state,
        Object event) {

      switch (eventName) {
        case "UpdateProduct":
          return view().updateProduct(
              state,
              (CustomerOrdersViewModel.ProductCreated) event);

        default:
          throw new UpdateHandlerNotFound(eventName);
      }
    }

  }


  @Override
  public ViewRouter<?, ?> viewRouter(String eventName) {
    switch (eventName) {
      case "UpdateCustomerCreated":
        return customersViewTableRouter;

      case "UpdateCustomerNameChanged":
        return customersViewTableRouter;

      case "UpdateProduct":
        return productsViewTableRouter;

      default:
        throw new UpdateHandlerNotFound(eventName);
    }
  }

}


