package org.example.view

import kalix.javasdk.impl.view.UpdateHandlerNotFound
import kalix.scalasdk.impl.view.ViewMultiTableRouter
import kalix.scalasdk.impl.view.ViewRouter
import kalix.scalasdk.view.View

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object CustomerOrdersViewRouter {
  class CustomersViewTableRouter(view: AbstractCustomerOrdersView#AbstractCustomersViewTable)
    extends ViewRouter[CustomerState, AbstractCustomerOrdersView#AbstractCustomersViewTable](view) {

    override def handleUpdate(
        eventName: String,
        state: CustomerState,
        event: Any): View.UpdateEffect[CustomerState] = {

      eventName match {
        case "UpdateCustomerCreated" =>
          view.updateCustomerCreated(
              state,
              event.asInstanceOf[CustomerCreated])

        case "UpdateCustomerNameChanged" =>
          view.updateCustomerNameChanged(
              state,
              event.asInstanceOf[CustomerNameChanged])

        case _ =>
          throw new UpdateHandlerNotFound(eventName)
      }
    }

  }

  class ProductsViewTableRouter(view: AbstractCustomerOrdersView#AbstractProductsViewTable)
    extends ViewRouter[ProductState, AbstractCustomerOrdersView#AbstractProductsViewTable](view) {

    override def handleUpdate(
        eventName: String,
        state: ProductState,
        event: Any): View.UpdateEffect[ProductState] = {

      eventName match {
        case "UpdateProduct" =>
          view.updateProduct(
              state,
              event.asInstanceOf[ProductCreated])

        case _ =>
          throw new UpdateHandlerNotFound(eventName)
      }
    }

  }
}

class CustomerOrdersViewRouter(view: CustomerOrdersView) extends ViewMultiTableRouter {

  private val customersViewTableRouter =
    new CustomerOrdersViewRouter.CustomersViewTableRouter(view.CustomersViewTable)

  private val productsViewTableRouter =
    new CustomerOrdersViewRouter.ProductsViewTableRouter(view.ProductsViewTable)

  override def viewRouter(eventName: String): ViewRouter[_, _] = {
    eventName match {
      case "UpdateCustomerCreated" =>
        customersViewTableRouter

      case "UpdateCustomerNameChanged" =>
        customersViewTableRouter

      case "UpdateProduct" =>
        productsViewTableRouter

      case _ =>
        throw new UpdateHandlerNotFound(eventName)
    }
  }

}

