package org.example.view

import kalix.javasdk.impl.view.UpdateHandlerNotFound
import kalix.scalasdk.impl.view.ViewRouter
import kalix.scalasdk.view.View

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

class CustomerOrdersViewRouter(view: CustomerOrdersView)
  extends ViewRouter[CustomerOrdersView](view) {

  override def handleUpdate[S](
      eventName: String,
      state: S,
      event: Any): View.UpdateEffect[S] = {

    eventName match {
      case "UpdateCustomerCreated" =>
        view.updateCustomerCreated(
          state.asInstanceOf[CustomerState],
          event.asInstanceOf[CustomerCreated]
        ).asInstanceOf[View.UpdateEffect[S]]

      case "UpdateCustomerNameChanged" =>
        view.updateCustomerNameChanged(
          state.asInstanceOf[CustomerState],
          event.asInstanceOf[CustomerNameChanged]
        ).asInstanceOf[View.UpdateEffect[S]]

      case "UpdateProduct" =>
        view.updateProduct(
          state.asInstanceOf[ProductState],
          event.asInstanceOf[ProductCreated]
        ).asInstanceOf[View.UpdateEffect[S]]

      case _ =>
        throw new UpdateHandlerNotFound(eventName)
    }
  }

  override def viewTable(eventName: String, event: Any): String = {
    eventName match {
      case "UpdateCustomerCreated" => "customers"
      case "UpdateCustomerNameChanged" => "customers"
      case "UpdateProduct" => "products"
      case _ => ""
    }
  }

}
