package org.example.view

import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CustomerOrdersView(context: ViewContext) extends AbstractCustomerOrdersView {

  override def emptyState: Any = {
    updateContext.viewTable match {
      case "customers" =>
        throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state for 'customers'");
      case "products" =>
        throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state for 'products'");
      case _ => null
    }
  }

  override def updateCustomerCreated(
    state: CustomerState, customerCreated: CustomerCreated): UpdateEffect[CustomerState] =
    throw new UnsupportedOperationException("Update handler for 'UpdateCustomerCreated' not implemented yet")

  override def updateCustomerNameChanged(
    state: CustomerState, customerNameChanged: CustomerNameChanged): UpdateEffect[CustomerState] =
    throw new UnsupportedOperationException("Update handler for 'UpdateCustomerNameChanged' not implemented yet")

  override def updateProduct(
    state: ProductState, productCreated: ProductCreated): UpdateEffect[ProductState] =
    throw new UnsupportedOperationException("Update handler for 'UpdateProduct' not implemented yet")
}
