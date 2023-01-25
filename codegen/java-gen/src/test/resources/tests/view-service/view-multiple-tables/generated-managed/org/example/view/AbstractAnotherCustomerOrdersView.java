package org.example.view;

import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewContext;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractAnotherCustomerOrdersView {

  private AbstractCustomersViewTable customersViewTable;
  private AbstractProductsViewTable productsViewTable;

  public AbstractAnotherCustomerOrdersView(ViewContext context) {
    customersViewTable = createCustomersViewTable(context);
    productsViewTable = createProductsViewTable(context);
  }

  public AbstractCustomersViewTable customersViewTable() {
    return customersViewTable;
  }

  public abstract AbstractCustomersViewTable createCustomersViewTable(ViewContext context);

  public static abstract class AbstractCustomersViewTable extends View<CustomerOrdersViewModel.Customer> {

    public abstract View.UpdateEffect<CustomerOrdersViewModel.Customer> updateCustomerCreated(
        CustomerOrdersViewModel.Customer state,
        CustomerOrdersViewModel.CustomerCreated customerCreated);

    public abstract View.UpdateEffect<CustomerOrdersViewModel.Customer> updateCustomerNameChanged(
        CustomerOrdersViewModel.Customer state,
        CustomerOrdersViewModel.CustomerNameChanged customerNameChanged);

  }

  public AbstractProductsViewTable productsViewTable() {
    return productsViewTable;
  }

  public abstract AbstractProductsViewTable createProductsViewTable(ViewContext context);

  public static abstract class AbstractProductsViewTable extends View<CustomerOrdersViewModel.Product> {

    public abstract View.UpdateEffect<CustomerOrdersViewModel.Product> updateProduct(
        CustomerOrdersViewModel.Product state,
        CustomerOrdersViewModel.ProductCreated productCreated);

  }

}

