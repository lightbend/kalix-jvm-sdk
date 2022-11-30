package org.example.view

import kalix.scalasdk.view.View

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractCustomerOrdersView {

  def CustomersViewTable: AbstractCustomersViewTable

  abstract class AbstractCustomersViewTable extends View[CustomerState] {
    def updateCustomerCreated(
        state: CustomerState,
        customerCreated: CustomerCreated): View.UpdateEffect[CustomerState]

    def updateCustomerNameChanged(
        state: CustomerState,
        customerNameChanged: CustomerNameChanged): View.UpdateEffect[CustomerState]
  }

  def ProductsViewTable: AbstractProductsViewTable

  abstract class AbstractProductsViewTable extends View[ProductState] {
    def updateProduct(
        state: ProductState,
        productCreated: ProductCreated): View.UpdateEffect[ProductState]
  }

}
