package org.example.view;

import kalix.javasdk.view.View;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractCustomerOrdersView extends View {

  public abstract View.UpdateEffect<CustomerOrdersViewModel.CustomerState> updateCustomerCreated(
    CustomerOrdersViewModel.CustomerState state, CustomerOrdersViewModel.CustomerCreated customerCreated);

  public abstract View.UpdateEffect<CustomerOrdersViewModel.CustomerState> updateCustomerNameChanged(
    CustomerOrdersViewModel.CustomerState state, CustomerOrdersViewModel.CustomerNameChanged customerNameChanged);

  public abstract View.UpdateEffect<CustomerOrdersViewModel.ProductState> updateProduct(
    CustomerOrdersViewModel.ProductState state, CustomerOrdersViewModel.ProductCreated productCreated);

}
